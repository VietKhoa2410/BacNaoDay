package demo.bacnaoday.api.payload;

import demo.bacnaoday.model.PersonRelationType;

public record PersonResponse(Long id, String displayName, PersonRelationType relationType, String toPerson) {}
