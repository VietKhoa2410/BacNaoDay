package demo.bacnaoday.service;

import demo.bacnaoday.api.payload.PersonResponse;
import demo.bacnaoday.model.Person;
import demo.bacnaoday.repository.PersonRepository;
import demo.bacnaoday.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final RelationPageService relationPageService;

    public PersonService(PersonRepository personRepository, RelationPageService relationPageService) {
        this.personRepository = personRepository;
        this.relationPageService = relationPageService;
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> listForPage(AppUserDetails user, Long pageId) {
        relationPageService.requireOwnedPage(user, pageId);
        return personRepository.findByRelationPage_IdOrderBySortOrderAscIdAsc(pageId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PersonResponse create(AppUserDetails user, Long pageId, String displayName, Integer sortOrder) {
        if (!StringUtils.hasText(displayName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        String trimmed = displayName.trim();
        if (trimmed.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is too long");
        }
        var page = relationPageService.requireOwnedPage(user, pageId);
        Person person = new Person();
        person.setRelationPage(page);
        person.setDisplayName(trimmed);
        person.setSortOrder(sortOrder);
        Instant now = Instant.now();
        person.setCreatedAt(now);
        person.setUpdatedAt(now);
        return toResponse(personRepository.save(person));
    }

    private PersonResponse toResponse(Person person) {
        return new PersonResponse(person.getId(), person.getDisplayName(), person.getSortOrder());
    }
}
