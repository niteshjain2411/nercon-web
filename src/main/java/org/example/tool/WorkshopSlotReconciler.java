package org.example.tool;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * One-time repair for workshop slot counts, for use after the fixes in dd283f7
 * and a2bad74.
 *
 * Two things need repairing in existing data:
 *
 *   1. bookedSlots may be inflated, because the old approval path incremented on
 *      every approval with no record of what it had already counted.
 *   2. Registrations approved before dd283f7 deployed have no countedWorkshops
 *      field. The new code reads that field to decide what has been counted, so
 *      without it a re-approval counts their workshops again, and a rejection
 *      releases nothing.
 *
 * The target state this restores is deliberately simple and self-consistent:
 *
 *   - an approved registration holds every workshop it has selected
 *   - a registration in any other state holds nothing
 *   - bookedSlots equals the number of approved registrations holding that workshop
 *
 * One consequence worth knowing: a delegate who was approved and has since added
 * a workshop that is still awaiting re-approval will have that new workshop
 * counted here, reserving the slot slightly earlier than the approval flow would.
 * That is the safe direction — it cannot oversell a workshop — and the approval
 * then becomes a no-op for slots.
 *
 * Inert unless --reconcile-workshops is passed, so normal deployments are
 * unaffected. Reports by default and changes nothing; pass --apply to write.
 *
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--reconcile-workshops"
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--reconcile-workshops --apply"
 */
@Component
public class WorkshopSlotReconciler implements ApplicationRunner {

    private static final String TRIGGER_OPTION = "reconcile-workshops";
    private static final String APPLY_OPTION = "apply";

    private static final String REGISTRATIONS = "Nercon26";
    private static final String WORKSHOPS = "nerconWS";
    private static final String COUNTED_WORKSHOPS_FIELD = "countedWorkshops";
    private static final String APPROVED = "approved";
    private static final String NO_WORKSHOP_SENTINEL = "ws0";

    /** Firestore caps a batch at 500 writes. */
    private static final int BATCH_LIMIT = 400;

    private final Firestore firestore;
    private final ConfigurableApplicationContext context;

    public WorkshopSlotReconciler(Firestore firestore, ConfigurableApplicationContext context) {
        this.firestore = firestore;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption(TRIGGER_OPTION)) return;
        boolean apply = args.containsOption(APPLY_OPTION);

        List<RegistrationState> registrations = loadRegistrations();
        Map<String, Long> currentSlots = loadWorkshopSlots();
        Plan plan = buildPlan(registrations, currentSlots);

        print(plan, registrations.size(), apply);
        if (apply) {
            write(plan);
            System.out.println("\nApplied. Re-run without --apply to confirm everything now reads 'in sync'.");
        } else {
            System.out.println("\nDry run - nothing was written. Re-run with --apply to make these changes.");
        }

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    // -------------------------------------------------------------------
    // Planning - pure, so it can be tested without Firestore
    // -------------------------------------------------------------------

    /** A registration as it matters here. countedWorkshops is null when the field is absent. */
    public record RegistrationState(String delegateId, String regstatus,
                                    List<String> workshops, List<String> countedWorkshops) {
    }

    public record Plan(Map<String, Long> currentSlots,
                       Map<String, Long> expectedSlots,
                       Map<String, List<String>> ledgerUpdates,
                       List<String> unknownWorkshops) {

        public Map<String, Long> slotChanges() {
            Map<String, Long> changed = new TreeMap<>();
            expectedSlots.forEach((ws, expected) -> {
                if (!expected.equals(currentSlots.get(ws))) changed.put(ws, expected);
            });
            return changed;
        }

        public boolean inSync() {
            return slotChanges().isEmpty() && ledgerUpdates.isEmpty();
        }
    }

    static Plan buildPlan(List<RegistrationState> registrations, Map<String, Long> currentSlots) {
        Map<String, Long> expected = new TreeMap<>();
        currentSlots.keySet().forEach(ws -> expected.put(ws, 0L));

        Map<String, List<String>> ledgerUpdates = new LinkedHashMap<>();
        Set<String> unknownWorkshops = new LinkedHashSet<>();

        for (RegistrationState reg : registrations) {
            boolean approved = APPROVED.equalsIgnoreCase(reg.regstatus());
            List<String> held = approved ? normalise(reg.workshops()) : List.of();

            for (String ws : held) {
                if (currentSlots.containsKey(ws)) {
                    expected.merge(ws, 1L, Long::sum);
                } else {
                    // Counted nowhere, because there is no workshop document to count into
                    unknownWorkshops.add(ws);
                }
            }

            // Rewrite the ledger only where it does not already say exactly this
            List<String> currentLedger = reg.countedWorkshops() == null
                    ? null
                    : normalise(reg.countedWorkshops());
            if (!held.equals(currentLedger)) {
                ledgerUpdates.put(reg.delegateId(), held);
            }
        }

        return new Plan(new TreeMap<>(currentSlots), expected, ledgerUpdates,
                new ArrayList<>(unknownWorkshops));
    }

    /** Drops blanks, the ws0 sentinel and duplicates, preserving order. */
    private static List<String> normalise(List<String> workshops) {
        if (workshops == null) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        for (String ws : workshops) {
            if (ws == null || ws.isBlank() || NO_WORKSHOP_SENTINEL.equals(ws)) continue;
            seen.add(ws);
        }
        return new ArrayList<>(seen);
    }

    // -------------------------------------------------------------------
    // Firestore access
    // -------------------------------------------------------------------

    private List<RegistrationState> loadRegistrations() throws Exception {
        List<RegistrationState> out = new ArrayList<>();
        for (QueryDocumentSnapshot doc : firestore.collection(REGISTRATIONS).get().get().getDocuments()) {
            out.add(new RegistrationState(
                    doc.getId(),
                    asString(doc.get("regstatus")),
                    asStringList(doc.get("workshops")),
                    // null vs empty matters: absent means never touched by the new code
                    doc.contains(COUNTED_WORKSHOPS_FIELD)
                            ? asStringList(doc.get(COUNTED_WORKSHOPS_FIELD))
                            : null));
        }
        return out;
    }

    private Map<String, Long> loadWorkshopSlots() throws Exception {
        Map<String, Long> out = new TreeMap<>();
        for (QueryDocumentSnapshot doc : firestore.collection(WORKSHOPS).get().get().getDocuments()) {
            Object booked = doc.get("bookedSlots");
            out.put(doc.getId(), booked instanceof Number n ? n.longValue() : 0L);
        }
        return out;
    }

    private void write(Plan plan) throws Exception {
        WriteBatch batch = firestore.batch();
        int queued = 0;

        for (Map.Entry<String, Long> e : plan.slotChanges().entrySet()) {
            batch.update(firestore.collection(WORKSHOPS).document(e.getKey()), "bookedSlots", e.getValue());
            if (++queued >= BATCH_LIMIT) {
                batch.commit().get();
                batch = firestore.batch();
                queued = 0;
            }
        }
        for (Map.Entry<String, List<String>> e : plan.ledgerUpdates().entrySet()) {
            batch.update(firestore.collection(REGISTRATIONS).document(e.getKey()),
                    COUNTED_WORKSHOPS_FIELD, e.getValue());
            if (++queued >= BATCH_LIMIT) {
                batch.commit().get();
                batch = firestore.batch();
                queued = 0;
            }
        }
        if (queued > 0) batch.commit().get();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) out.add(item.toString());
            }
        }
        return out;
    }

    // -------------------------------------------------------------------
    // Reporting
    // -------------------------------------------------------------------

    private void print(Plan plan, int registrationCount, boolean apply) {
        System.out.println();
        System.out.println("Workshop slot reconciliation " + (apply ? "(APPLY)" : "(dry run)"));
        System.out.println("=".repeat(64));
        System.out.printf("Registrations scanned: %d   Workshops: %d%n",
                registrationCount, plan.currentSlots().size());

        System.out.println("\nbookedSlots");
        System.out.printf("  %-16s %10s %10s %8s%n", "workshop", "current", "expected", "delta");
        plan.expectedSlots().forEach((ws, expected) -> {
            long current = plan.currentSlots().getOrDefault(ws, 0L);
            long delta = expected - current;
            System.out.printf("  %-16s %10d %10d %+8d%s%n",
                    ws, current, expected, delta, delta == 0 ? "" : "   <-- changes");
        });

        System.out.printf("%ncountedWorkshops backfill: %d registration(s)%n", plan.ledgerUpdates().size());
        plan.ledgerUpdates().entrySet().stream().limit(20).forEach(e ->
                System.out.printf("  %-14s -> %s%n", e.getKey(),
                        e.getValue().isEmpty() ? "[] (holds nothing)" : e.getValue()));
        if (plan.ledgerUpdates().size() > 20) {
            System.out.printf("  ... and %d more%n", plan.ledgerUpdates().size() - 20);
        }

        if (!plan.unknownWorkshops().isEmpty()) {
            System.out.println("\nWARNING - registrations reference workshops with no nerconWS document:");
            plan.unknownWorkshops().forEach(ws -> System.out.println("  " + ws));
            System.out.println("  These cannot be counted. Check for a typo or a deleted workshop.");
        }

        if (plan.inSync()) {
            System.out.println("\nEverything is already in sync - nothing to do.");
        }
    }
}
