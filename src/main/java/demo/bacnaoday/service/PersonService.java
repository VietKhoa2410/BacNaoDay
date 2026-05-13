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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        var page = relationPageService.requireOwnedPage(user, pageId);
        Long markedPersonId =
                page.getMarkedPerson() == null ? null : page.getMarkedPerson().getId();
        List<Person> people = personRepository.findByRelationPage_IdOrderByIdAsc(pageId);
        List<PersonGraphEdgeResponse> edges =
                personRelationService.listForRelationPage(pageId).stream()
                        .map(
                                pr ->
                                        new PersonGraphEdgeResponse(
                                                pr.getFromPerson().getId(),
                                                pr.getToPerson().getId(),
                                                pr.getRelationType()))
                        .toList();
        Set<Long> idSet = people.stream().map(Person::getId).collect(Collectors.toCollection(HashSet::new));
        Map<Long, Person> byId = people.stream().collect(Collectors.toMap(Person::getId, p -> p));
        List<Long> order = PersonGraphTopology.orderPersonIdsByFamilyAbove(idSet, edges);
        List<PersonGraphNodeResponse> nodes =
                order.stream()
                        .map(id -> {
                            Person p = byId.get(id);
                            return new PersonGraphNodeResponse(p.getId(), p.getDisplayName());
                        })
                        .toList();
        return new PersonGraphResponse(nodes, edges, markedPersonId);
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

    @Transactional
    public void delete(AppUserDetails user, Long pageId, Long personId) {
        if (personId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personId is required");
        }
        var page = relationPageService.requireOwnedPage(user, pageId);
        if (!personRepository.existsByIdAndRelationPage_Id(personId, pageId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personId does not belong to this relation page");
        }

        Long markedPersonId = page.getMarkedPerson() == null ? null : page.getMarkedPerson().getId();
        if (personId.equals(markedPersonId)) {
            page.setMarkedPerson(null);
        }
        page.setUpdatedAt(Instant.now());

        personRelationService.deleteAllForPerson(personId);
        personRepository.deleteById(personId);
    }

    private PersonResponse toResponse(Person person, PersonRelationType relationType, String toPersonName) {
        return new PersonResponse(person.getId(), person.getDisplayName(), relationType, toPersonName);
    }

    private PersonResponse toResponse(Person person) {
        return new PersonResponse(person.getId(), person.getDisplayName(), null, null);
    }
}
