package com.brifo.server.diary.share

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Configuration
@EnableConfigurationProperties(DiaryShareImageProperties::class)
class ShareImageStorageConfig {
    @Bean
    fun shareImageS3Client(properties: DiaryShareImageProperties): S3Client {
        val builder =
            S3Client
                .builder()
                .region(Region.of(properties.region))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(
                    S3Configuration
                        .builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess)
                        .build(),
                )

        properties.endpoint.takeIf(String::isNotBlank)?.let {
            builder.endpointOverride(URI.create(it))
        }

        return builder.build()
    }
}
