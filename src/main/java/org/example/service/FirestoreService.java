package org.example.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.example.model.RegistrationData;
import org.example.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    private static final String COLLECTION_NAME = "Nercon26";

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

            // Default null image fields to empty string
            if (registration.getPaymentimg() == null) registration.setPaymentimg("");
            if (registration.getPgbonafideimg() == null) registration.setPgbonafideimg("");

            DocumentReference docRef = firestore
                    .collection(COLLECTION_NAME)
                    .document(registration.getDelegateId());

            ApiFuture<WriteResult> future = docRef.set(toFirestoreMap(registration));
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
     * Update the regstatus field for a registration document.
     */
    public void updateRegistrationStatus(String delegateId, String status)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(delegateId);
        ApiFuture<WriteResult> future = docRef.update("regstatus", status);
        future.get();
    }

    /**
     * Update workshops and append a new transaction for an existing registration.
     * Personal details and regstatus are NOT touched.
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
        if (totalAmount != null && !totalAmount.isBlank()) {
            updates.put("totalAmount", totalAmount);
        }
        if (txnid != null && !txnid.isBlank()) {
            Map<String, String> txn = new LinkedHashMap<>();
            txn.put("txnid", txnid);
            txn.put("txndate", txndate != null ? txndate : "");
            updates.put("txndetails." + txnKey, txn);
        }
        if (paymentimgUrl != null && !paymentimgUrl.isBlank()) {
            updates.put("paymentimg", paymentimgUrl);
        }

        docRef.update(updates).get();
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
        data.put("regstatus", reg.getRegstatus());
        data.put("medcouncilregnum", reg.getMedcouncilregnum());
        data.put("isattendworkshop", reg.isAttendworkshop());
        data.put("workshops", reg.getWorkshops() != null ? reg.getWorkshops() : List.of());
        data.put("accompanycount", reg.getAccompanycount());
        data.put("totalAmount", reg.getTotalAmount());
        data.put("delegateId", reg.getDelegateId());
        data.put("paymentimg", reg.getPaymentimg());
        data.put("pgbonafideimg", reg.getPgbonafideimg());
        data.put("synopsis", reg.getSynopsis() != null ? reg.getSynopsis() : "");

        // Serialize txndetails as a nested map: { "key": { txnid, txndate } }
        if (reg.getTxndetails() != null) {
            Map<String, Object> txnMap = new LinkedHashMap<>();
            reg.getTxndetails().forEach((key, txn) -> {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("txnid", txn.getTxnid());
                entry.put("txndate", txn.getTxndate());
                txnMap.put(key, entry);
            });
            data.put("txndetails", txnMap);
        } else {
            data.put("txndetails", Map.of());
        }

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
        reg.setRegstatus(getString(data, "regstatus"));
        reg.setMedcouncilregnum(getString(data, "medcouncilregnum"));
        reg.setAttendworkshop(Boolean.TRUE.equals(data.get("isattendworkshop")));
        reg.setWorkshops((List<String>) data.getOrDefault("workshops", List.of()));
        Object accompany = data.get("accompanycount");
        reg.setAccompanycount(accompany instanceof Number ? ((Number) accompany).longValue() : 0L);
        reg.setTotalAmount(getString(data, "totalAmount"));
        reg.setPaymentimg(getString(data, "paymentimg"));
        reg.setPgbonafideimg(getString(data, "pgbonafideimg"));
        reg.setSynopsis(getString(data, "synopsis"));

        // Deserialize txndetails
        Object txnRaw = data.get("txndetails");
        if (txnRaw instanceof Map<?, ?> txnRawMap) {
            Map<String, Transaction> txndetails = new LinkedHashMap<>();
            txnRawMap.forEach((k, v) -> {
                if (v instanceof Map<?, ?> entryMap) {
                    Transaction txn = new Transaction(
                            getString((Map<String, Object>) entryMap, "txnid"),
                            getString((Map<String, Object>) entryMap, "txndate")
                    );
                    txndetails.put(String.valueOf(k), txn);
                }
            });
            reg.setTxndetails(txndetails);
        }

        return reg;
    }

    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
