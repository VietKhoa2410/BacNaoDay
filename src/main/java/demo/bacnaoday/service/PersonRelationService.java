package demo.bacnaoday.service;

import demo.bacnaoday.model.Person;
import demo.bacnaoday.model.PersonRelation;
import demo.bacnaoday.model.PersonRelationType;
import demo.bacnaoday.repository.PersonRelationRepository;
import demo.bacnaoday.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonRelationService {

    private final PersonRelationRepository personRelationRepository;
    private final PersonRepository personRepository;

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

    @Transactional(readOnly = true)
    public List<PersonRelation> listForRelationPage(Long pageId) {
        return personRelationRepository.findByFromPerson_RelationPage_IdOrderByIdAsc(pageId);
    }

    @Transactional(readOnly = true)
    public int computeRelativeLevel(Long pageId, Long fromPersonId, Long toPersonId) {
        if (fromPersonId.equals(toPersonId)) {
            return 0;
        }
        List<PersonRelation> relations = listForRelationPage(pageId);

        Map<Long, List<long[]>> adj = new HashMap<>();
        for (PersonRelation r : relations) {
            Long u = r.getFromPerson().getId();
            Long v = r.getToPerson().getId();
            if (u == null || v == null || u.equals(v)) {
                continue;
            }
            int delta =
                    switch (r.getRelationType()) {
                        case FATHER_OF, MOTHER_OF, PARENT_OF -> 1;
                        case SON_OF, DAUGHTER_OF, CHILD_OF -> -1;
                        case SPOUSE_OF, SIBLING_OF -> 0;
                    };
            adj.computeIfAbsent(u, k -> new java.util.ArrayList<>()).add(new long[] {v, delta});
        }

        Queue<Long> q = new ArrayDeque<>();
        Map<Long, Integer> dist = new HashMap<>();
        Map<Long, Integer> level = new HashMap<>();
        Set<Long> inQueue = new HashSet<>();

        dist.put(fromPersonId, 0);
        level.put(fromPersonId, 0);
        q.add(fromPersonId);
        inQueue.add(fromPersonId);

        while (!q.isEmpty()) {
            Long u = q.poll();
            inQueue.remove(u);
            int du = dist.get(u);
            int lu = level.get(u);
            if (u.equals(toPersonId)) {
                return lu;
            }
            for (long[] e : adj.getOrDefault(u, List.of())) {
                long vLong = e[0];
                int delta = (int) e[1];
                Long v = vLong;
                int nd = du + 1;
                int nl = lu + delta;
                Integer bestDist = dist.get(v);
                if (bestDist == null || nd < bestDist) {
                    dist.put(v, nd);
                    level.put(v, nl);
                    if (inQueue.add(v)) {
                        q.add(v);
                    }
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no relation path between marked person and target");
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
