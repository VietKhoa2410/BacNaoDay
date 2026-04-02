package demo.bacnaoday.service;

import demo.bacnaoday.api.payload.PersonGraphEdgeResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Orders person ids so that ancestors appear before descendants (top-to-bottom), using only
 * parent/child relation edges. Spouse and sibling edges do not impose vertical order.
 */
final class PersonGraphTopology {

    private PersonGraphTopology() {}

    static List<Long> orderPersonIdsByFamilyAbove(Set<Long> personIds, List<PersonGraphEdgeResponse> edges) {
        if (personIds.isEmpty()) {
            return List.of();
        }

        Set<String> constraintKeys = new HashSet<>();
        List<long[]> constraints = new ArrayList<>();
        for (PersonGraphEdgeResponse e : edges) {
            Long before = null;
            Long after = null;
            switch (e.relationType()) {
                case FATHER_OF, MOTHER_OF, PARENT_OF -> {
                    before = e.fromPersonId();
                    after = e.toPersonId();
                }
                case SON_OF, DAUGHTER_OF, CHILD_OF -> {
                    before = e.toPersonId();
                    after = e.fromPersonId();
                }
                default -> {
                    /* SPOUSE_OF, SIBLING_OF — no vertical constraint */
                }
            }
            if (before == null || after == null || before.equals(after)) {
                continue;
            }
            if (!personIds.contains(before) || !personIds.contains(after)) {
                continue;
            }
            String key = before + "\0" + after;
            if (constraintKeys.add(key)) {
                constraints.add(new long[] {before, after});
            }
        }

        Map<Long, List<Long>> successors = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        for (Long id : personIds) {
            inDegree.put(id, 0);
        }
        for (long[] c : constraints) {
            long u = c[0];
            long v = c[1];
            successors.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            inDegree.merge(v, 1, Integer::sum);
        }

        PriorityQueue<Long> ready = new PriorityQueue<>();
        for (Map.Entry<Long, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }

        List<Long> ordered = new ArrayList<>(personIds.size());
        while (!ready.isEmpty()) {
            Long u = ready.poll();
            ordered.add(u);
            for (Long v : successors.getOrDefault(u, List.of())) {
                int d = inDegree.merge(v, -1, Integer::sum);
                if (d == 0) {
                    ready.add(v);
                }
            }
        }

        if (ordered.size() < personIds.size()) {
            List<Long> rest = new ArrayList<>();
            for (Long id : personIds) {
                if (!ordered.contains(id)) {
                    rest.add(id);
                }
            }
            rest.sort(Long::compareTo);
            ordered.addAll(rest);
        }

        return ordered;
    }
}
