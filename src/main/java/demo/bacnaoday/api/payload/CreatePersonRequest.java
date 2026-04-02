package demo.bacnaoday.api.payload;

import demo.bacnaoday.model.PersonRelationType;

public record CreatePersonRequest(String displayName, Long toPersonId, PersonRelationType relationType) {}
