package com.brifo.server.term.entity

import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.util.UUID

@Entity
@Table(name = "glossary_terms")
class GlossaryTerm private constructor(
    term: String,
    definition: String,
    category: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "glossaryTermIdGenerator")
    @SequenceGenerator(name = "glossaryTermIdGenerator", sequenceName = "glossary_terms_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @Column(name = "term", nullable = false, length = 80)
    var term: String = term
        protected set

    @Column(name = "definition", nullable = false, length = 200)
    var definition: String = definition
        protected set

    @Column(name = "category", nullable = false, length = 50)
    var category: String = category
        protected set

    companion object {
        fun create(
            term: String,
            definition: String,
            category: String,
        ): GlossaryTerm {
            return GlossaryTerm(
                term = term,
                definition = definition,
                category = category,
            )
        }
    }
}
