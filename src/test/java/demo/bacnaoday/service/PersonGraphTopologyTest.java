package demo.bacnaoday.service;

import demo.bacnaoday.api.payload.PersonGraphEdgeResponse;
import demo.bacnaoday.model.PersonRelationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PersonGraphTopologyTest {

    @Test
    void ordersGrandparentParentChildChain() {
        long c = 3L;
        long a = 2L;
        long b = 1L;
        Set<Long> ids = Set.of(c, a, b);
        List<PersonGraphEdgeResponse> edges =
                List.of(
                        edge(c, a, PersonRelationType.MOTHER_OF),
                        edge(a, c, PersonRelationType.SON_OF),
                        edge(a, b, PersonRelationType.FATHER_OF),
                        edge(b, a, PersonRelationType.SON_OF));
        List<Long> order = PersonGraphTopology.orderPersonIdsByFamilyAbove(ids, edges);
        assertThat(order).containsExactly(c, a, b);
    }

    @Test
    void parentBeforeChildWhenOnlyForwardParentEdge() {
        long parent = 10L;
        long child = 20L;
        List<PersonGraphEdgeResponse> edges = List.of(edge(parent, child, PersonRelationType.FATHER_OF));
        List<Long> order =
                PersonGraphTopology.orderPersonIdsByFamilyAbove(Set.of(parent, child), edges);
        assertThat(order).containsExactly(parent, child);
    }

    @Test
    void tieBreaksByIdWhenSameRank() {
        long x = 5L;
        long y = 7L;
        List<Long> order = PersonGraphTopology.orderPersonIdsByFamilyAbove(Set.of(y, x), List.of());
        assertThat(order).containsExactly(x, y);
    }

    private static PersonGraphEdgeResponse edge(long from, long to, PersonRelationType type) {
        return new PersonGraphEdgeResponse(from, to, type);
    }
}
