package demo.bacnaoday.service;

import demo.bacnaoday.model.Person;
import demo.bacnaoday.model.PersonRelation;
import demo.bacnaoday.model.PersonRelationType;
import demo.bacnaoday.repository.PersonRelationRepository;
import demo.bacnaoday.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonRelationService {

    private final PersonRelationRepository personRelationRepository;
    private final PersonRepository personRepository;

    public PersonRelationService(
            PersonRelationRepository personRelationRepository, PersonRepository personRepository) {
        this.personRelationRepository = personRelationRepository;
        this.personRepository = personRepository;
    }

    /**
     * Persists two rows: {@code fromPerson → toPerson} with {@code forwardType}, and
     * {@code toPerson → fromPerson} with the inverse type derived from genders per product rules.
     */
    @Transactional
    public void createBidirectional(Long fromPersonId, Long toPersonId, PersonRelationType forwardType) {
        if (fromPersonId.equals(toPersonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromPersonId and toPersonId must differ");
        }
        Person from = requireActivePerson(fromPersonId);
        Person to = requireActivePerson(toPersonId);
        if (!from.getRelationPage().getId().equals(to.getRelationPage().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "both persons must belong to the same relation page");
        }

        PersonRelationType inverseType = forwardType.inverseForForwardEdge(from, to);

        if (personRelationRepository.existsByFromPerson_IdAndToPerson_IdAndRelationType(
                fromPersonId, toPersonId, forwardType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "this relation already exists");
        }
        if (personRelationRepository.existsByFromPerson_IdAndToPerson_IdAndRelationType(
                toPersonId, fromPersonId, inverseType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "inverse relation already exists");
        }

        PersonRelation forward = new PersonRelation();
        forward.setFromPerson(from);
        forward.setToPerson(to);
        forward.setRelationType(forwardType);

        PersonRelation backward = new PersonRelation();
        backward.setFromPerson(to);
        backward.setToPerson(from);
        backward.setRelationType(inverseType);

        personRelationRepository.save(forward);
        personRelationRepository.save(backward);
    }

    private Person requireActivePerson(Long id) {
        Person person =
                personRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (person.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "person is deleted");
        }
        return person;
    }
}
