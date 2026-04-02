package demo.bacnaoday.repository;

import demo.bacnaoday.model.PersonRelation;
import demo.bacnaoday.model.PersonRelationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRelationRepository extends JpaRepository<PersonRelation, Long> {

    boolean existsByFromPerson_IdAndToPerson_IdAndRelationType(
            Long fromPersonId, Long toPersonId, PersonRelationType relationType);
}
