package com.brifo.server.diary.share

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@EnableConfigurationProperties(DiaryShareImageProperties::class)
class ShareImageStorageConfig {
    @Bean
    fun shareImageS3Client(properties: DiaryShareImageProperties): S3Client {
        return S3Client
            .builder()
            .region(Region.of(properties.region))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build()
    }

    @Bean
    fun shareImageS3Presigner(properties: DiaryShareImageProperties): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(properties.region))
            .build()
}
