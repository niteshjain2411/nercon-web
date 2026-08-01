package org.example.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.example.model.RegistrationData;
import org.example.model.Transaction;
import org.example.model.Workshop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class FirestoreService {

    private static final String COLLECTION_NAME = "Nercon26";

    /**
     * Registration field holding the workshop IDs whose bookedSlots have already
     * been incremented for this delegate. Deliberately not part of RegistrationData:
     * it is bookkeeping owned by the approval path, not registration data.
     */
    private static final String COUNTED_WORKSHOPS_FIELD = "countedWorkshops";

    private final Firestore firestore;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    public FirestoreService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Save or overwrite a registration document in Firestore.
     * The document ID equals delegateId.
     */
    public boolean saveRegistration(RegistrationData registration) {
        try {
            // Auto-derive isattendworkshop from workshops list (exclude ws0 = "no workshop")
            List<String> workshops = registration.getWorkshops();
            boolean attendsWorkshop = workshops != null && !workshops.isEmpty()
                    && !(workshops.size() == 1 && "ws0".equals(workshops.get(0)));
            registration.setAttendworkshop(attendsWorkshop);

            if (registration.getPgbonafideimg() == null) registration.setPgbonafideimg("");

            DocumentReference docRef = firestore
                    .collection(COLLECTION_NAME)
                    .document(registration.getDelegateId());

            // Merge rather than replace: toFirestoreMap covers every registration field,
            // so this writes the same values, but it preserves approval bookkeeping such
            // as countedWorkshops that a re-save would otherwise wipe.
            ApiFuture<WriteResult> future = docRef.set(toFirestoreMap(registration), SetOptions.merge());
            future.get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            System.err.println("Firestore save error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetch all documents from the Nercon26 collection.
     */
    public List<RegistrationData> getAllRegistrations() {
        List<RegistrationData> list = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                RegistrationData reg = fromFirestoreMap(doc.getData(), doc.getId());
                list.add(reg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("Firestore fetch error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Update the regstatus for all transactions associated with a delegate.
     * Falls back to updating the legacy regstatus field on the registration document
     * for old-format records that pre-date the nerconTrx split.
     */
    public void updateRegistrationStatus(String delegateId, String status)
            throws ExecutionException, InterruptedException {
        RegistrationData reg = getRegistrationById(delegateId);
        if (reg == null) return;

        // Always update regstatus on the registration document itself
        firestore.collection(COLLECTION_NAME).document(delegateId)
                .set(Map.of("regstatus", status), com.google.cloud.firestore.SetOptions.merge()).get();

        // Also update each linked transaction document (set+merge so missing docs don't throw)
        if (reg.getTxndetails() != null) {
            for (String txnId : reg.getTxndetails()) {
                if (txnId != null && !txnId.isBlank()) {
                    firestore.collection("nerconTrx").document(txnId)
                            .set(Map.of("regstatus", status), com.google.cloud.firestore.SetOptions.merge()).get();
                }
            }
        }
    }

    /**
     * Increment bookedSlots by 1 for each workshop the delegate selected that has
     * not already been counted for this delegate.
     *
     * Every workshop counted is recorded in the registration's countedWorkshops
     * field, so approving the same registration twice — or re-approving it after
     * the delegate adds a workshop — never counts a workshop a second time. The
     * whole operation runs in one transaction, so concurrent approvals of the same
     * delegate cannot both count the same workshop.
     *
     * ws0 (the "no workshop" sentinel) is skipped, as is any workshop with no
     * nerconWS document, so a stale ID cannot fail an approval that has already
     * written the status and sent the email.
     *
     * @return the workshop IDs actually incremented by this call
     */
    public List<String> incrementWorkshopBookedSlots(String delegateId, List<String> workshopIds)
            throws ExecutionException, InterruptedException {
        if (delegateId == null || delegateId.isBlank() || workshopIds == null || workshopIds.isEmpty()) {
            return List.of();
        }
        DocumentReference regRef = firestore.collection(COLLECTION_NAME).document(delegateId);

        return firestore.runTransaction(txn -> {
            DocumentSnapshot regSnap = txn.get(regRef).get();
            if (!regSnap.exists()) return List.<String>of();

            Set<String> alreadyCounted = new HashSet<>();
            Object counted = regSnap.get(COUNTED_WORKSHOPS_FIELD);
            if (counted instanceof List<?> countedList) {
                for (Object id : countedList) {
                    if (id != null) alreadyCounted.add(id.toString());
                }
            }

            // Candidates: skip ws0, duplicates within the request, and anything already counted
            List<String> candidates = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String wsId : workshopIds) {
                if (wsId == null || wsId.isBlank() || "ws0".equals(wsId)) continue;
                if (alreadyCounted.contains(wsId) || !seen.add(wsId)) continue;
                candidates.add(wsId);
            }
            if (candidates.isEmpty()) return List.<String>of();

            // A Firestore transaction requires every read before any write
            List<String> toIncrement = new ArrayList<>();
            for (String wsId : candidates) {
                if (txn.get(firestore.collection("nerconWS").document(wsId)).get().exists()) {
                    toIncrement.add(wsId);
                } else {
                    System.err.println("Skipping unknown workshop '" + wsId + "' for " + delegateId);
                }
            }
            if (toIncrement.isEmpty()) return List.<String>of();

            for (String wsId : toIncrement) {
                txn.update(firestore.collection("nerconWS").document(wsId),
                        "bookedSlots", FieldValue.increment(1));
            }
            txn.update(regRef, COUNTED_WORKSHOPS_FIELD, FieldValue.arrayUnion(toIncrement.toArray()));
            return toIncrement;
        }).get();
    }

    /**
     * Give back every workshop slot currently counted for a delegate, used when a
     * registration is rejected after having been approved.
     *
     * Decrements bookedSlots for each workshop listed in countedWorkshops and then
     * clears that list, so the registration holds no slots afterwards. Because the
     * list is what drives the work, rejecting twice releases nothing the second
     * time, and a delegate who is approved again is counted again from scratch:
     * approve, reject, approve leaves bookedSlots exactly one higher, not two.
     *
     * bookedSlots is floored at zero so an already-corrupt counter cannot be driven
     * negative, and a workshop with no nerconWS document is skipped rather than
     * failing a rejection whose status has already been written.
     *
     * @return the workshop IDs actually released by this call
     */
    public List<String> releaseWorkshopBookedSlots(String delegateId)
            throws ExecutionException, InterruptedException {
        if (delegateId == null || delegateId.isBlank()) return List.of();
        DocumentReference regRef = firestore.collection(COLLECTION_NAME).document(delegateId);

        return firestore.runTransaction(txn -> {
            DocumentSnapshot regSnap = txn.get(regRef).get();
            if (!regSnap.exists()) return List.<String>of();

            // Only slots this delegate actually holds are given back
            List<String> held = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            Object counted = regSnap.get(COUNTED_WORKSHOPS_FIELD);
            if (counted instanceof List<?> countedList) {
                for (Object id : countedList) {
                    if (id == null) continue;
                    String wsId = id.toString();
                    if (wsId.isBlank() || !seen.add(wsId)) continue;
                    held.add(wsId);
                }
            }
            if (held.isEmpty()) return List.<String>of();

            // A Firestore transaction requires every read before any write
            Map<String, Long> currentSlots = new LinkedHashMap<>();
            for (String wsId : held) {
                DocumentSnapshot wsSnap = txn.get(firestore.collection("nerconWS").document(wsId)).get();
                if (wsSnap.exists()) {
                    Object booked = wsSnap.get("bookedSlots");
                    currentSlots.put(wsId, booked instanceof Number n ? n.longValue() : 0L);
                } else {
                    System.err.println("Skipping unknown workshop '" + wsId + "' while releasing " + delegateId);
                }
            }

            List<String> released = new ArrayList<>();
            for (Map.Entry<String, Long> entry : currentSlots.entrySet()) {
                // Explicit floored value rather than increment(-1): the read above makes
                // this atomic within the transaction and keeps the counter non-negative
                txn.update(firestore.collection("nerconWS").document(entry.getKey()),
                        "bookedSlots", Math.max(0L, entry.getValue() - 1));
                released.add(entry.getKey());
            }

            // Clear the ledger even if some workshops were skipped, so the registration
            // is not left holding slots that can never be released
            txn.update(regRef, COUNTED_WORKSHOPS_FIELD, List.of());
            return released;
        }).get();
    }

    /**
     * Update workshops and append a new transaction ID for an existing registration.
     * Personal details are NOT touched.
     */
    public void updateWorkshops(String delegateId, List<String> workshops,
                                String txnKey, String txnid, String txndate,
                                String paymentimgUrl, String totalAmount)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(delegateId);

        boolean attendsWorkshop = workshops != null && !workshops.isEmpty()
                && !(workshops.size() == 1 && "ws0".equals(workshops.get(0)));

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("workshops", workshops != null ? workshops : List.of());
        updates.put("isattendworkshop", attendsWorkshop);
        if (txnid != null && !txnid.isBlank()) {
            // Append new txnid to the txndetails list atomically
            updates.put("txndetails", FieldValue.arrayUnion(txnid));
            // Save the transaction record to nerconTrx
            saveTransaction(new Transaction(txnid, txndate != null ? txndate : "",
                    paymentimgUrl != null ? paymentimgUrl : "",
                    totalAmount != null ? totalAmount : "", "pending"));
        }

        docRef.update(updates).get();
    }

    /**
     * Save a Transaction record to the nerconTrx collection.
     */
    public void saveTransaction(Transaction txn) throws ExecutionException, InterruptedException {
        if (txn == null || txn.getTxnid() == null || txn.getTxnid().isBlank()) return;
        Map<String, Object> txnMap = new LinkedHashMap<>();
        txnMap.put("txnid", txn.getTxnid());
        txnMap.put("txndate", txn.getTxndate() != null ? txn.getTxndate() : "");
        txnMap.put("paymentimg", txn.getPaymentimg() != null ? txn.getPaymentimg() : "");
        txnMap.put("totalAmount", txn.getTotalAmount() != null ? txn.getTotalAmount() : "");
        txnMap.put("regstatus", txn.getRegstatus() != null ? txn.getRegstatus() : "pending");
        firestore.collection("nerconTrx").document(txn.getTxnid()).set(txnMap).get();
    }

    /**
     * Fetch all transactions from nerconTrx.
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        try {
            for (QueryDocumentSnapshot doc : firestore.collection("nerconTrx").get().get().getDocuments()) {
                Map<String, Object> d = doc.getData();
                Transaction txn = new Transaction(
                        doc.getId(),
                        getString(d, "txndate"),
                        getString(d, "paymentimg"),
                        getString(d, "totalAmount"),
                        getString(d, "regstatus"));
                list.add(txn);
            }
        } catch (Exception e) {
            System.err.println("Firestore fetch transactions error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Fetch all workshops from nerconWS.
     */
    public List<Workshop> getAllWorkshops() {
        List<Workshop> list = new ArrayList<>();
        try {
            QuerySnapshot snapshot = firestore.collection("nerconWS").get().get(15, TimeUnit.SECONDS);
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> d = doc.getData();
                Workshop ws = new Workshop();
                ws.setId(doc.getId());
                ws.setContent(getString(d, "content"));
                ws.setDurationType(getString(d, "durationType"));
                ws.setResourcePerson(getString(d, "resourcePerson"));
                Object maxS = d.get("maxSlots");
                ws.setMaxSlots(maxS instanceof Number ? ((Number) maxS).longValue() : 0L);
                Object bookS = d.get("bookedSlots");
                ws.setBookedSlots(bookS instanceof Number ? ((Number) bookS).longValue() : 0L);
                list.add(ws);
            }
        } catch (TimeoutException e) {
            System.err.println("Firestore fetch workshops timed out: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Firestore fetch workshops error: " + e.getClass().getName() + ": " + e.getMessage());
        }
        return list;
    }

    /**
     * Fetch a single registration document by delegateId.
     * Returns null if the document does not exist.
     */
    public RegistrationData getRegistrationById(String delegateId)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(delegateId);
        DocumentSnapshot snapshot = docRef.get().get();
        if (!snapshot.exists()) return null;
        return fromFirestoreMap(Objects.requireNonNull(snapshot.getData()), snapshot.getId());
    }

    /**
     * Upload an image to Firebase Storage and return its public URL.
     *
     * @param file       the multipart file to upload
     * @param delegateId used to scope the storage path
     * @param imageType  "payment" or "bonafide"
     */
    public String uploadImage(MultipartFile file, String delegateId, String imageType) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1)
                : "jpg";
        String objectPath = "registrations/" + delegateId + "/" + imageType + "." + extension;

        Bucket bucket = StorageClient.getInstance().bucket();
        Blob blob = bucket.create(objectPath, file.getInputStream(), file.getContentType());

        // Make the uploaded file publicly readable
        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

        return "https://storage.googleapis.com/" + storageBucket + "/" + objectPath;
    }

    // -------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------

    private Map<String, Object> toFirestoreMap(RegistrationData reg) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fullname", reg.getFullname());
        data.put("email", reg.getEmail());
        data.put("phone", reg.getPhone());
        data.put("gender", reg.getGender());
        data.put("institute", reg.getInstitute());
        data.put("city", reg.getCity());
        data.put("state", reg.getState());
        data.put("designation", reg.getDesignation());
        data.put("medcouncil", reg.getMedcouncil());
        data.put("medcouncilregnum", reg.getMedcouncilregnum());
        data.put("isattendworkshop", reg.isAttendworkshop());
        data.put("workshops", reg.getWorkshops() != null ? reg.getWorkshops() : List.of());
        data.put("accompanycount", reg.getAccompanycount());
        data.put("delegateId", reg.getDelegateId());
        data.put("pgbonafideimg", reg.getPgbonafideimg() != null ? reg.getPgbonafideimg() : "");
        data.put("synopsis", reg.getSynopsis() != null ? reg.getSynopsis() : "");
        data.put("registrationCategory", reg.getRegistrationCategory() != null ? reg.getRegistrationCategory() : "");
        data.put("creationTS", reg.getCreationTS() != null ? reg.getCreationTS() : "");
        data.put("txndetails", reg.getTxndetails() != null ? reg.getTxndetails() : List.of());
        return data;
    }

    @SuppressWarnings("unchecked")
    private RegistrationData fromFirestoreMap(Map<String, Object> data, String docId) {
        RegistrationData reg = new RegistrationData();
        reg.setDelegateId(docId);
        reg.setFullname(getString(data, "fullname"));
        reg.setEmail(getString(data, "email"));
        reg.setPhone(getString(data, "phone"));
        reg.setGender(getString(data, "gender"));
        reg.setInstitute(getString(data, "institute"));
        reg.setCity(getString(data, "city"));
        reg.setState(getString(data, "state"));
        reg.setDesignation(getString(data, "designation"));
        reg.setMedcouncil(getString(data, "medcouncil"));
        reg.setMedcouncilregnum(getString(data, "medcouncilregnum"));
        reg.setAttendworkshop(Boolean.TRUE.equals(data.get("isattendworkshop")));

        // workshops: stored as list of strings (IDs)
        Object wsRaw = data.get("workshops");
        if (wsRaw instanceof List<?> wsList) {
            List<String> workshops = new ArrayList<>();
            for (Object item : wsList) {
                if (item != null) workshops.add(item.toString());
            }
            reg.setWorkshops(workshops);
        } else {
            reg.setWorkshops(new ArrayList<>());
        }

        Object accompany = data.get("accompanycount");
        if (accompany instanceof Number) {
            reg.setAccompanycount(((Number) accompany).longValue());
        } else if (accompany instanceof String s) {
            try { reg.setAccompanycount(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) { reg.setAccompanycount(0L); }
        } else {
            reg.setAccompanycount(0L);
        }
        reg.setPgbonafideimg(getString(data, "pgbonafideimg"));
        reg.setSynopsis(getString(data, "synopsis"));
        reg.setRegistrationCategory(getString(data, "registrationCategory"));
        reg.setCreationTS(getString(data, "creationTS"));

        // txndetails: list of strings, or legacy map format
        Object txnRaw = data.get("txndetails");
        if (txnRaw instanceof List<?> txnList) {
            List<String> ids = new ArrayList<>();
            for (Object item : txnList) {
                if (item != null) ids.add(item.toString());
            }
            reg.setTxndetails(ids);
        } else if (txnRaw instanceof Map<?, ?> legacyMap) {
            // Legacy: stored as { "txn1": { txnid: "...", txndate: "..." } }
            List<String> ids = new ArrayList<>();
            legacyMap.forEach((k, v) -> {
                if (v instanceof Map<?, ?> entry) {
                    Object txnid = entry.get("txnid");
                    if (txnid != null) ids.add(txnid.toString());
                } else if (v != null) {
                    ids.add(v.toString());
                }
            });
            reg.setTxndetails(ids);
        } else {
            reg.setTxndetails(new ArrayList<>());
        }

        // Legacy fallback: transaction fields stored directly on old registration documents
        reg.setTxnid(getString(data, "txnid"));
        reg.setTxndate(getString(data, "txndate"));
        reg.setTotalAmount(getString(data, "totalAmount"));
        reg.setRegstatus(getString(data, "regstatus"));
        reg.setPaymentimg(getString(data, "paymentimg"));

        return reg;
    }

    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
