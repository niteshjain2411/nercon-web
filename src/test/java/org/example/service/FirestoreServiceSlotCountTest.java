package org.example.service;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the idempotency of workshop slot counting on approval.
 *
 * The bug these guard against: the approval endpoint incremented bookedSlots for
 * every workshop on the registration each time it ran, so approving twice — or
 * re-approving after a delegate added a second workshop — counted the original
 * workshop again.
 */
class FirestoreServiceSlotCountTest {

    private static final String DELEGATE = "DEL001";

    private Firestore firestore;
    private FirestoreService service;
    private com.google.cloud.firestore.Transaction txn;

    private DocumentReference regRef;
    private Map<String, DocumentReference> wsRefs;
    private Map<String, DocumentSnapshot> wsSnaps;
    private DocumentSnapshot regSnap;

    /** Workshop IDs that have a nerconWS document. */
    private final List<String> existingWorkshops = new ArrayList<>(List.of("ws1", "ws2", "ws3"));

    @BeforeEach
    void setUp() throws Exception {
        firestore = mock(Firestore.class);
        txn = mock(com.google.cloud.firestore.Transaction.class);
        service = new FirestoreService(firestore);

        regRef = mock(DocumentReference.class);
        regSnap = mock(DocumentSnapshot.class);
        when(regSnap.exists()).thenReturn(true);

        CollectionReference regCol = mock(CollectionReference.class);
        when(firestore.collection("Nercon26")).thenReturn(regCol);
        when(regCol.document(DELEGATE)).thenReturn(regRef);

        // nerconWS: a DocumentReference per workshop id, each with its own snapshot
        wsRefs = new HashMap<>();
        wsSnaps = new HashMap<>();
        CollectionReference wsCol = mock(CollectionReference.class);
        when(firestore.collection("nerconWS")).thenReturn(wsCol);
        for (String id : List.of("ws0", "ws1", "ws2", "ws3", "ws-missing")) {
            DocumentReference ref = mock(DocumentReference.class);
            wsRefs.put(id, ref);
            when(wsCol.document(id)).thenReturn(ref);

            DocumentSnapshot snap = mock(DocumentSnapshot.class);
            wsSnaps.put(id, snap);
            when(snap.exists()).thenReturn(existingWorkshops.contains(id));
            when(snap.get("bookedSlots")).thenReturn(5L); // arbitrary non-zero default
            when(txn.get(ref)).thenReturn(ApiFutures.immediateFuture(snap));
        }

        when(txn.get(regRef)).thenReturn(ApiFutures.immediateFuture(regSnap));

        // Run the transaction body inline against the mock transaction
        when(firestore.runTransaction(any())).thenAnswer(inv -> {
            com.google.cloud.firestore.Transaction.Function<?> fn = inv.getArgument(0);
            return ApiFutures.immediateFuture(fn.updateCallback(txn));
        });
    }

    /** Sets what countedWorkshops already holds on the registration document. */
    private void alreadyCounted(List<String> ids) {
        when(regSnap.get("countedWorkshops")).thenReturn(ids);
    }

    private void verifyIncremented(String wsId) {
        verify(txn).update(eq(wsRefs.get(wsId)), eq("bookedSlots"), any(FieldValue.class));
    }

    private void verifyNotIncremented(String wsId) {
        verify(txn, never()).update(eq(wsRefs.get(wsId)), eq("bookedSlots"), any(FieldValue.class));
    }

    @Test
    @DisplayName("first approval counts every selected workshop")
    void firstApproval_countsAll() throws Exception {
        alreadyCounted(null);

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1", "ws2"));

        assertEquals(List.of("ws1", "ws2"), counted);
        verifyIncremented("ws1");
        verifyIncremented("ws2");
    }

    @Test
    @DisplayName("approving the same registration twice does not count anything again")
    void secondApproval_countsNothing() throws Exception {
        alreadyCounted(List.of("ws1", "ws2"));

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1", "ws2"));

        assertTrue(counted.isEmpty(), "re-approval must not count any workshop again");
        verifyNotIncremented("ws1");
        verifyNotIncremented("ws2");
        verify(txn, never()).update(eq(regRef), eq("countedWorkshops"), any());
    }

    @Test
    @DisplayName("re-approval after adding a workshop counts only the new one")
    void reApprovalAfterAddingWorkshop_countsOnlyTheNewOne() throws Exception {
        // Delegate registered with ws1 and was approved, then added ws2 and is approved again
        alreadyCounted(List.of("ws1"));

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1", "ws2"));

        assertEquals(List.of("ws2"), counted, "ws1 was already counted and must not count twice");
        verifyIncremented("ws2");
        verifyNotIncremented("ws1");
    }

    @Test
    @DisplayName("ws0 sentinel is never counted")
    void ws0_isSkipped() throws Exception {
        alreadyCounted(null);

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws0"));

        assertTrue(counted.isEmpty());
        verifyNotIncremented("ws0");
    }

    @Test
    @DisplayName("a duplicate id in one request is counted once")
    void duplicateIdsInOneRequest_countOnce() throws Exception {
        alreadyCounted(null);

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1", "ws1"));

        assertEquals(List.of("ws1"), counted);
        verify(txn).update(eq(wsRefs.get("ws1")), eq("bookedSlots"), any(FieldValue.class));
    }

    @Test
    @DisplayName("an unknown workshop id is skipped instead of failing the approval")
    void unknownWorkshop_isSkipped() throws Exception {
        alreadyCounted(null);

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws-missing", "ws3"));

        assertEquals(List.of("ws3"), counted);
        verifyIncremented("ws3");
        verifyNotIncremented("ws-missing");
    }

    @Test
    @DisplayName("nothing is written when the registration document is gone")
    void missingRegistration_writesNothing() throws Exception {
        when(regSnap.exists()).thenReturn(false);

        List<String> counted = service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1"));

        assertTrue(counted.isEmpty());
        verifyNotIncremented("ws1");
    }

    @Test
    @DisplayName("no transaction is started for an empty or null workshop list")
    void emptyInput_startsNoTransaction() throws Exception {
        assertTrue(service.incrementWorkshopBookedSlots(DELEGATE, List.of()).isEmpty());
        assertTrue(service.incrementWorkshopBookedSlots(DELEGATE, null).isEmpty());
        assertTrue(service.incrementWorkshopBookedSlots(null, List.of("ws1")).isEmpty());

        verify(firestore, never()).runTransaction(any());
    }

    // ---------------------------------------------------------------
    // Releasing slots on rejection
    // ---------------------------------------------------------------

    private void bookedSlots(String wsId, long value) {
        when(wsSnaps.get(wsId).get("bookedSlots")).thenReturn(value);
    }

    private void verifyBookedSlotsSetTo(String wsId, long expected) {
        verify(txn).update(eq(wsRefs.get(wsId)), eq("bookedSlots"), eq(expected));
    }

    @Test
    @DisplayName("rejecting an approved registration gives its slots back")
    void reject_releasesCountedSlots() throws Exception {
        alreadyCounted(List.of("ws1", "ws2"));
        bookedSlots("ws1", 5L);
        bookedSlots("ws2", 3L);

        List<String> released = service.releaseWorkshopBookedSlots(DELEGATE);

        assertEquals(List.of("ws1", "ws2"), released);
        verifyBookedSlotsSetTo("ws1", 4L);
        verifyBookedSlotsSetTo("ws2", 2L);
    }

    @Test
    @DisplayName("the ledger is cleared so the registration holds no slots afterwards")
    void reject_clearsLedger() throws Exception {
        alreadyCounted(List.of("ws1"));

        service.releaseWorkshopBookedSlots(DELEGATE);

        verify(txn).update(eq(regRef), eq("countedWorkshops"), eq(List.of()));
    }

    @Test
    @DisplayName("rejecting twice releases nothing the second time")
    void rejectTwice_releasesOnce() throws Exception {
        // After the first rejection the ledger is empty
        alreadyCounted(List.of());

        List<String> released = service.releaseWorkshopBookedSlots(DELEGATE);

        assertTrue(released.isEmpty(), "a second rejection must not give slots back again");
        verify(txn, never()).update(eq(wsRefs.get("ws1")), eq("bookedSlots"), any());
    }

    @Test
    @DisplayName("rejecting a registration that was never approved releases nothing")
    void rejectWithoutApproval_releasesNothing() throws Exception {
        alreadyCounted(null);

        assertTrue(service.releaseWorkshopBookedSlots(DELEGATE).isEmpty());
        verify(txn, never()).update(eq(wsRefs.get("ws1")), eq("bookedSlots"), any());
    }

    @Test
    @DisplayName("approve, reject, approve leaves the count one higher, not two")
    void approveRejectApprove_netsOne() throws Exception {
        // Approve: nothing counted yet, so ws1 is counted
        alreadyCounted(null);
        assertEquals(List.of("ws1"), service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1")));
        verify(txn).update(eq(wsRefs.get("ws1")), eq("bookedSlots"), any(FieldValue.class));

        // Reject: the ledger now holds ws1, so it is given back
        alreadyCounted(List.of("ws1"));
        bookedSlots("ws1", 6L);
        assertEquals(List.of("ws1"), service.releaseWorkshopBookedSlots(DELEGATE));
        verifyBookedSlotsSetTo("ws1", 5L);

        // Approve again: the ledger was cleared, so ws1 counts once more
        alreadyCounted(List.of());
        assertEquals(List.of("ws1"), service.incrementWorkshopBookedSlots(DELEGATE, List.of("ws1")));
    }

    @Test
    @DisplayName("bookedSlots is floored at zero rather than going negative")
    void release_flooredAtZero() throws Exception {
        alreadyCounted(List.of("ws1"));
        bookedSlots("ws1", 0L);

        service.releaseWorkshopBookedSlots(DELEGATE);

        verifyBookedSlotsSetTo("ws1", 0L);
    }

    @Test
    @DisplayName("an unknown workshop is skipped but the ledger is still cleared")
    void release_skipsUnknownWorkshopAndStillClearsLedger() throws Exception {
        alreadyCounted(List.of("ws-missing", "ws1"));
        bookedSlots("ws1", 2L);

        List<String> released = service.releaseWorkshopBookedSlots(DELEGATE);

        assertEquals(List.of("ws1"), released);
        verifyBookedSlotsSetTo("ws1", 1L);
        verify(txn, never()).update(eq(wsRefs.get("ws-missing")), eq("bookedSlots"), any());
        // Otherwise the registration would hold a slot it could never release
        verify(txn).update(eq(regRef), eq("countedWorkshops"), eq(List.of()));
    }

    @Test
    @DisplayName("no transaction is started when releasing without a delegateId")
    void release_blankDelegate_startsNoTransaction() throws Exception {
        assertTrue(service.releaseWorkshopBookedSlots(null).isEmpty());
        assertTrue(service.releaseWorkshopBookedSlots("  ").isEmpty());

        verify(firestore, never()).runTransaction(any());
    }
}
