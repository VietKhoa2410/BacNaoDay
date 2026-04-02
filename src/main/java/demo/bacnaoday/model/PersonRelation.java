package demo.bacnaoday.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "person_relations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_person_relations_from_to_type",
                        columnNames = {"from_person_id", "to_person_id", "relation_type"}),
        indexes = {
            @Index(name = "idx_person_relations_from", columnList = "from_person_id"),
            @Index(name = "idx_person_relations_to", columnList = "to_person_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class PersonRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_person_id", nullable = false)
    private Person fromPerson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_person_id", nullable = false)
    private Person toPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 32)
    private PersonRelationType relationType;
}
