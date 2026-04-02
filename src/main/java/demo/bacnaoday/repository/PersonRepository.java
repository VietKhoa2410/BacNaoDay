package demo.bacnaoday.repository;

import demo.bacnaoday.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByRelationPage_IdOrderByIdAsc(Long relationPageId);
}
