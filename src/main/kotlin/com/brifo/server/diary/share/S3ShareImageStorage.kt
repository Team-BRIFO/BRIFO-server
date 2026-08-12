package com.brifo.server.diary.share

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Component
class S3ShareImageStorage(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val properties: DiaryShareImageProperties,
) : ShareImageStorage {
    override fun store(
        diaryId: UUID,
        image: ShareImageFile,
    ): String {
        check(properties.bucket.isNotBlank() && properties.bucket != "disabled") {
            "share image bucket is not configured"
        }
        val key = "$diaryId.${image.extension}"
        val request =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(image.contentType)
                .build()

        s3Client.putObject(request, RequestBody.fromBytes(image.bytes))
        return key
    }

    override fun createDownloadUrl(key: String): String {
        check(properties.bucket.isNotBlank() && properties.bucket != "disabled") {
            "share image bucket is not configured"
        }
        val getObjectRequest =
            GetObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .build()
        val presignRequest =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(PRESIGNED_URL_DURATION)
                .getObjectRequest(getObjectRequest)
                .build()
        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    companion object {
        private val PRESIGNED_URL_DURATION: Duration = Duration.ofMinutes(15)
    }
}
