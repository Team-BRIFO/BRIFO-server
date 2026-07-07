package com.brifo.server.policy.repository

import com.brifo.server.policy.entity.UserPolicy
import org.springframework.data.jpa.repository.JpaRepository

interface UserPolicyRepository : JpaRepository<UserPolicy, Long>
