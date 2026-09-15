# 빌더는 빌드 머신의 네이티브 아키텍처에서 돌린다.
# 자바 바이트코드는 아키텍처 독립적이므로 arm64 이미지를 만들 때도
# Gradle을 arm64로 에뮬레이션할 이유가 없다. 이 지정이 없으면 amd64 러너에서
# arm64 빌드 시 컴파일 전체가 QEMU로 돌아 20분 제한을 넘긴다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk-noble AS builder

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew bootJar --no-daemon && \
    find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" \
        -exec cp {} app.jar \;

FROM eclipse-temurin:21-jre-noble

ENV TZ=Asia/Seoul
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul"

RUN apt-get update && \
    apt-get install --yes --no-install-recommends fontconfig fonts-noto-cjk && \
    rm -rf /var/lib/apt/lists/* && \
    fc-match "Noto Sans CJK KR" | grep --quiet "NotoSansCJK" && \
    groupadd --system app && \
    useradd --system --gid app --home-dir /app app

WORKDIR /app

COPY --from=builder --chown=app:app /workspace/app.jar ./app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
