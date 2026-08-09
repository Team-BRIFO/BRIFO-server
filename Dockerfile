FROM eclipse-temurin:21-jdk-noble AS builder

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

RUN groupadd --system app && \
    useradd --system --gid app --home-dir /app app

WORKDIR /app

COPY --from=builder --chown=app:app /workspace/app.jar ./app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
