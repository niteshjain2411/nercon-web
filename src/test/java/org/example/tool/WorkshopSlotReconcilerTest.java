package org.example.tool;

import org.example.tool.WorkshopSlotReconciler.Plan;
import org.example.tool.WorkshopSlotReconciler.RegistrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reconciler rewrites live slot counts, so the planning step is worth
 * pinning down precisely. Every case here describes data the production
 * collection can actually contain after the old double-counting bug.
 */
class WorkshopSlotReconcilerTest {

    private static Map<String, Long> slots(Object... pairs) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
        }
        return map;
    }

    /** Approved registration whose countedWorkshops field is absent (pre-deploy). */
    private static RegistrationState legacyApproved(String id, String... workshops) {
        return new RegistrationState(id, "approved", List.of(workshops), null);
    }

    @Test
    @DisplayName("an inflated count is corrected to the number of approved holders")
    void inflatedCount_isCorrected() {
        // ws1 reads 12 but only 10 approved delegates hold it
        List<RegistrationState> regs = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) regs.add(legacyApproved("DEL" + i, "ws1"));

        Plan plan = WorkshopSlotReconciler.buildPlan(regs, slots("ws1", 12));

        assertEquals(10L, plan.expectedSlots().get("ws1"));
        assertEquals(Map.of("ws1", 10L), plan.slotChanges());
        assertFalse(plan.inSync());
    }

    @Test
    @DisplayName("pre-deploy approvals get their ledger backfilled")
    void legacyApproved_isBackfilled() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(legacyApproved("DEL1", "ws1", "ws2")), slots("ws1", 1, "ws2", 1));

        assertEquals(List.of("ws1", "ws2"), plan.ledgerUpdates().get("DEL1"));
    }

    @Test
    @DisplayName("a registration already holding exactly the right slots is left alone")
    void correctLedger_isNotRewritten() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(new RegistrationState("DEL1", "approved", List.of("ws1"), List.of("ws1"))),
                slots("ws1", 1));

        assertTrue(plan.ledgerUpdates().isEmpty(), "no write should be queued for correct data");
        assertTrue(plan.inSync());
    }

    @Test
    @DisplayName("a rejected registration is stripped of any slots it still held")
    void rejectedWithStaleLedger_isCleared() {
        // Approved then rejected under the old code, which never released anything
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(new RegistrationState("DEL1", "rejected", List.of("ws1"), List.of("ws1"))),
                slots("ws1", 1));

        assertEquals(List.of(), plan.ledgerUpdates().get("DEL1"), "a rejected delegate holds nothing");
        assertEquals(0L, plan.expectedSlots().get("ws1"));
        assertEquals(Map.of("ws1", 0L), plan.slotChanges());
    }

    @Test
    @DisplayName("a pending registration does not count towards any workshop")
    void pendingRegistration_holdsNothing() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(new RegistrationState("DEL1", "pending", List.of("ws1"), null)),
                slots("ws1", 1));

        assertEquals(0L, plan.expectedSlots().get("ws1"));
    }

    @Test
    @DisplayName("the ws0 sentinel never counts and never lands in a ledger")
    void ws0_isIgnored() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(legacyApproved("DEL1", "ws0")), slots("ws1", 0));

        assertEquals(List.of(), plan.ledgerUpdates().get("DEL1"));
        assertEquals(0L, plan.expectedSlots().get("ws1"));
    }

    @Test
    @DisplayName("a workshop listed twice on one registration counts once")
    void duplicateWorkshop_countsOnce() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(legacyApproved("DEL1", "ws1", "ws1")), slots("ws1", 5));

        assertEquals(1L, plan.expectedSlots().get("ws1"));
        assertEquals(List.of("ws1"), plan.ledgerUpdates().get("DEL1"));
    }

    @Test
    @DisplayName("a workshop with no document is reported rather than silently dropped")
    void unknownWorkshop_isReported() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(legacyApproved("DEL1", "ws-typo", "ws1")), slots("ws1", 1));

        assertEquals(List.of("ws-typo"), plan.unknownWorkshops());
        assertEquals(1L, plan.expectedSlots().get("ws1"));
        // It still enters the ledger, so the count stays consistent if the document reappears
        assertEquals(List.of("ws-typo", "ws1"), plan.ledgerUpdates().get("DEL1"));
    }

    @Test
    @DisplayName("a workshop nobody holds is driven to zero, not left untouched")
    void workshopWithNoHolders_goesToZero() {
        Plan plan = WorkshopSlotReconciler.buildPlan(List.of(), slots("ws1", 4));

        assertEquals(0L, plan.expectedSlots().get("ws1"));
        assertEquals(Map.of("ws1", 0L), plan.slotChanges());
    }

    @Test
    @DisplayName("an approved delegate awaiting re-approval for a new workshop reserves it")
    void approvedWithPendingAddition_reservesTheNewWorkshop() {
        // Approved holding ws1, then added ws2 which has not been re-approved yet
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(new RegistrationState("DEL1", "approved", List.of("ws1", "ws2"), List.of("ws1"))),
                slots("ws1", 1, "ws2", 0));

        assertEquals(1L, plan.expectedSlots().get("ws2"));
        assertEquals(List.of("ws1", "ws2"), plan.ledgerUpdates().get("DEL1"),
                "ledger and bookedSlots must agree, so the later approval is a no-op for slots");
    }

    @Test
    @DisplayName("a normal boot without the flag never touches Firestore")
    void withoutTheFlag_doesNothing() throws Exception {
        // This class ships inside the deployed app, so an ordinary Cloud Run start
        // must not read, write, or shut anything down.
        com.google.cloud.firestore.Firestore firestore =
                org.mockito.Mockito.mock(com.google.cloud.firestore.Firestore.class);
        org.springframework.context.ConfigurableApplicationContext context =
                org.mockito.Mockito.mock(org.springframework.context.ConfigurableApplicationContext.class);
        org.springframework.boot.ApplicationArguments args =
                org.mockito.Mockito.mock(org.springframework.boot.ApplicationArguments.class);
        org.mockito.Mockito.when(args.containsOption("reconcile-workshops")).thenReturn(false);

        new WorkshopSlotReconciler(firestore, context).run(args);

        org.mockito.Mockito.verifyNoInteractions(firestore, context);
    }

    @Test
    @DisplayName("clean data reports as in sync so a re-run is a safe confirmation")
    void cleanData_isInSync() {
        Plan plan = WorkshopSlotReconciler.buildPlan(
                List.of(new RegistrationState("DEL1", "approved", List.of("ws1"), List.of("ws1")),
                        new RegistrationState("DEL2", "rejected", List.of("ws1"), List.of())),
                slots("ws1", 1));

        assertTrue(plan.inSync());
        assertTrue(plan.slotChanges().isEmpty());
        assertTrue(plan.ledgerUpdates().isEmpty());
    }
}
