package com.brifo.server.ap.entity

import com.brifo.server.global.common.BaseEntity
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

@Entity
@Table(name = "attendance_rewards")
class AttendanceReward private constructor(
    user: User,
    consecutiveDays: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendanceRewardIdGenerator")
    @SequenceGenerator(
        name = "attendanceRewardIdGenerator",
        sequenceName = "attendance_rewards_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @Column(name = "consecutive_days", nullable = false)
    var consecutiveDays: Int = consecutiveDays
        protected set

    companion object {
        private const val REWARD_CYCLE_DAYS = 7

        fun create(
            user: User,
            consecutiveDays: Int,
        ): AttendanceReward {
            require(consecutiveDays in 1..REWARD_CYCLE_DAYS) {
                "consecutiveDays must be between 1 and $REWARD_CYCLE_DAYS"
            }

            return AttendanceReward(
                user = user,
                consecutiveDays = consecutiveDays,
            )
        }
    }
}
