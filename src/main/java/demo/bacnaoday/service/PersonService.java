package demo.bacnaoday.service;

import demo.bacnaoday.api.payload.CreatePersonRequest;
import demo.bacnaoday.api.payload.PersonGraphEdgeResponse;
import demo.bacnaoday.api.payload.PersonGraphNodeResponse;
import demo.bacnaoday.api.payload.PersonGraphResponse;
import demo.bacnaoday.api.payload.PersonResponse;
import demo.bacnaoday.model.Person;
import demo.bacnaoday.model.PersonRelationType;
import demo.bacnaoday.repository.PersonRepository;
import demo.bacnaoday.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final RelationPageService relationPageService;
    private final PersonRelationService personRelationService;

    public PersonService(
            PersonRepository personRepository,
            RelationPageService relationPageService,
            PersonRelationService personRelationService) {
        this.personRepository = personRepository;
        this.relationPageService = relationPageService;
        this.personRelationService = personRelationService;
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> listForPage(AppUserDetails user, Long pageId) {
        relationPageService.requireOwnedPage(user, pageId);
        return personRepository.findByRelationPage_IdOrderByIdAsc(pageId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonGraphResponse graphForPage(AppUserDetails user, Long pageId) {
        relationPageService.requireOwnedPage(user, pageId);
        List<PersonGraphNodeResponse> nodes =
                personRepository.findByRelationPage_IdOrderByIdAsc(pageId).stream()
                        .map(p -> new PersonGraphNodeResponse(p.getId(), p.getDisplayName()))
                        .toList();
        List<PersonGraphEdgeResponse> edges =
                personRelationService.listForRelationPage(pageId).stream()
                        .map(
                                pr ->
                                        new PersonGraphEdgeResponse(
                                                pr.getFromPerson().getId(),
                                                pr.getToPerson().getId(),
                                                pr.getRelationType()))
                        .toList();
        return new PersonGraphResponse(nodes, edges);
    }

    @Transactional
    public PersonResponse create(AppUserDetails user, Long pageId, CreatePersonRequest request) {
        String displayName = request.displayName();
        if (!StringUtils.hasText(displayName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        String trimmed = displayName.trim();
        if (trimmed.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is too long");
        }
        Long toPersonId = request.toPersonId();
        PersonRelationType relationType = request.relationType();
        boolean hasToPerson = toPersonId != null;
        boolean hasRelationType = relationType != null;

        var page = relationPageService.requireOwnedPage(user, pageId);
        Person person = new Person();
        person.setRelationPage(page);
        person.setDisplayName(trimmed);
        Instant now = Instant.now();
        person.setCreatedAt(now);
        person.setUpdatedAt(now);
        person = personRepository.save(person);

        if (hasToPerson &&  hasRelationType) {
            Optional<Person> toPersonOpt = personRepository.findById(toPersonId);
            if (toPersonOpt.isPresent()) {
                personRelationService.createBidirectional(person.getId(), toPersonId, relationType);
                return toResponse(person, relationType, toPersonOpt.get().getDisplayName());
            }
        }
        return toResponse(person);

    }

    private PersonResponse toResponse(Person person, PersonRelationType relationType, String toPersonName) {
        return new PersonResponse(person.getId(), person.getDisplayName(), relationType, toPersonName);
    }

    private PersonResponse toResponse(Person person) {
        return new PersonResponse(person.getId(), person.getDisplayName(), null, null);
    }
}
