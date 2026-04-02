package demo.bacnaoday.api.payload;

import java.util.List;

public record PersonGraphResponse(
        List<PersonGraphNodeResponse> nodes, List<PersonGraphEdgeResponse> edges, Long markedPersonId) {}
