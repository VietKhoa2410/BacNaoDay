package demo.bacnaoday.model;

import java.util.Objects;

/**
 * Stored on each directed {@code person_relations} row. When creating a logical kinship, the service
 * inserts the forward edge (from → to with this type) and the inverse edge (to → from) using
 * {@link #inverseForForwardEdge(Person, Person)}.
 */
public enum PersonRelationType {

    FATHER_OF,
    MOTHER_OF,
    /** Parent → child when parent gender is unknown. */
    PARENT_OF,

    SON_OF,
    DAUGHTER_OF,
    CHILD_OF,

    SPOUSE_OF,
    SIBLING_OF;

    /**
     * Forward edge: {@code from} → {@code to} with type {@code this}. Returns the type for the
     * inverse edge {@code to} → {@code from}. Parent/child roles follow the forward type (e.g.
     * {@code FATHER_OF}: {@code to} is the child; {@code SON_OF}: {@code to} is the parent).
     */
    public PersonRelationType inverseForForwardEdge(Person from, Person to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return switch (this) {
            case FATHER_OF, MOTHER_OF, PARENT_OF -> childToParentType(to.getGender());
            case SON_OF, DAUGHTER_OF, CHILD_OF -> parentToChildType(to.getGender());
            case SPOUSE_OF, SIBLING_OF -> this;
        };
    }

    private static PersonRelationType childToParentType(Gender childGender) {
        return switch (childGender) {
            case M -> SON_OF;
            case F -> DAUGHTER_OF;
            case UNKNOWN -> CHILD_OF;
        };
    }

    private static PersonRelationType parentToChildType(Gender parentGender) {
        return switch (parentGender) {
            case M -> FATHER_OF;
            case F -> MOTHER_OF;
            case UNKNOWN -> PARENT_OF;
        };
    }
}
