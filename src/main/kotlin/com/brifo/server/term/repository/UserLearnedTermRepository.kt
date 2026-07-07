package com.brifo.server.term.repository

import com.brifo.server.term.entity.UserLearnedTerm
import org.springframework.data.jpa.repository.JpaRepository

interface UserLearnedTermRepository : JpaRepository<UserLearnedTerm, Long>
