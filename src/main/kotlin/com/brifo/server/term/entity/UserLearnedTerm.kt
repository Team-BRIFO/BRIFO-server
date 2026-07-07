package com.brifo.server.term.entity

import com.brifo.server.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_learned_terms")
class UserLearnedTerm private constructor(
    user: User,
    term: GlossaryTerm,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userLearnedTermIdGenerator")
    @SequenceGenerator(
        name = "userLearnedTermIdGenerator",
        sequenceName = "user_learned_terms_id_seq",
        allocationSize = 1,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    var term: GlossaryTerm = term
        protected set

    @Column(name = "learned_at", nullable = false, insertable = false, updatable = false)
    var learnedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            user: User,
            term: GlossaryTerm,
        ): UserLearnedTerm {
            return UserLearnedTerm(
                user = user,
                term = term,
            )
        }
    }
}
