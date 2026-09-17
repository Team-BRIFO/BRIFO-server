package com.brifo.server.batch.generation

import org.slf4j.LoggerFactory
import org.springframework.batch.core.listener.SkipListener
import org.springframework.stereotype.Component

@Component
class NewsCardGenerationSkipListener : SkipListener<Long, GeneratedNewsCardItem> {
    override fun onSkipInProcess(
        newsId: Long,
        t: Throwable,
    ) {
        log.error("카드뉴스 생성에 실패해 이 뉴스는 건너뜁니다. newsId={}", newsId, t)
    }

    override fun onSkipInWrite(
        item: GeneratedNewsCardItem,
        t: Throwable,
    ) {
        log.error("카드뉴스 저장에 실패해 이 항목은 건너뜁니다. newsId={}", item.newsId, t)
    }

    override fun onSkipInRead(t: Throwable) {
        log.error("카드뉴스 생성 대상 조회 중 오류가 발생해 이 항목을 건너뜁니다.", t)
    }

    private companion object {
        val log = LoggerFactory.getLogger(NewsCardGenerationSkipListener::class.java)
    }
}
