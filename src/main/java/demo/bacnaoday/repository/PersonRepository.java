package demo.bacnaoday.repository;

import demo.bacnaoday.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByRelationPage_IdOrderByIdAsc(Long relationPageId);

    boolean existsByIdAndRelationPage_Id(Long id, Long relationPageId);

    Optional<Person> findByIdAndRelationPage_Owner_Id(Long id, Long ownerUserId);
}
