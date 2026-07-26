package com.brifo.server.policy.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.policy.entity.PolicyCode
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class PolicyActiveCodeConstraintTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `동일 코드의 활성 약관은 제목과 버전이 달라도 하나만 존재할 수 있다`() {
        insertPolicy(
            code = PolicyCode.TERMS_OF_SERVICE,
            title = "서비스 이용약관",
            version = "1.00",
            isActive = true,
        )

        assertThrows(PersistenceException::class.java) {
            insertPolicy(
                code = PolicyCode.TERMS_OF_SERVICE,
                title = "브리포 서비스 약관",
                version = "1.10",
                isActive = true,
            )
        }
    }

    @Test
    fun `동일 코드의 비활성 약관 이력은 제목이 바뀌어도 여러 버전을 유지할 수 있다`() {
        insertPolicy(
            code = PolicyCode.TERMS_OF_SERVICE,
            title = "서비스 이용약관",
            version = "1.00",
            isActive = false,
        )

        assertDoesNotThrow {
            insertPolicy(
                code = PolicyCode.TERMS_OF_SERVICE,
                title = "브리포 서비스 약관",
                version = "1.10",
                isActive = false,
            )
        }
    }

    private fun insertPolicy(
        code: PolicyCode,
        title: String,
        version: String,
        isActive: Boolean,
    ) {
        entityManager
            .createNativeQuery(
                """
                INSERT INTO policies (code, title, content, is_required, version, is_active)
                VALUES (:code, :title, '약관 전문', TRUE, CAST(:version AS NUMERIC), :isActive)
                """.trimIndent(),
            ).setParameter("code", code.name)
            .setParameter("title", title)
            .setParameter("version", version)
            .setParameter("isActive", isActive)
            .executeUpdate()
        entityManager.flush()
    }
}
