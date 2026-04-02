package demo.bacnaoday.api.payload;

import demo.bacnaoday.model.PersonRelationType;

public record PersonGraphEdgeResponse(Long fromPersonId, Long toPersonId, PersonRelationType relationType) {}
