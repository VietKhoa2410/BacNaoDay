package demo.bacnaoday.repository;

import demo.bacnaoday.model.RelationPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelationPageRepository extends JpaRepository<RelationPage, Long> {

    List<RelationPage> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    Optional<RelationPage> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByOwner_IdAndName(Long ownerId, String name);
}
