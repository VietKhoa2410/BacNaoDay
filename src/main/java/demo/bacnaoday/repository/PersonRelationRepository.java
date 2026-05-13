package demo.bacnaoday.repository;

import demo.bacnaoday.model.PersonRelation;
import demo.bacnaoday.model.PersonRelationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRelationRepository extends JpaRepository<PersonRelation, Long> {

    boolean existsByFromPerson_IdAndToPerson_IdAndRelationType(
            Long fromPersonId, Long toPersonId, PersonRelationType relationType);

    List<PersonRelation> findByFromPerson_RelationPage_IdOrderByIdAsc(Long relationPageId);

    long deleteByFromPerson_IdOrToPerson_Id(Long fromPersonId, Long toPersonId);
}
