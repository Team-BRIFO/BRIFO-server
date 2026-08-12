package com.brifo.server.diary.share

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Component
class S3ShareImageStorage(
    private val s3Client: S3Client,
    private val properties: DiaryShareImageProperties,
) : ShareImageStorage {
    override fun store(
        diaryId: UUID,
        image: ShareImageFile,
    ): String {
        check(properties.bucket.isNotBlank() && properties.bucket != "disabled") {
            "share image bucket is not configured"
        }
        check(properties.publicBaseUrl.isNotBlank()) {
            "share image public base URL is not configured"
        }

        val key = "${properties.keyPrefix.trim('/')}/$diaryId.${image.extension}"
        val request =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(image.contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build()

        s3Client.putObject(request, RequestBody.fromBytes(image.bytes))
        return "${properties.publicBaseUrl.trimEnd('/')}/$key"
    }
}
