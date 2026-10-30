FROM gradle:9.1.0-jdk25 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates ca-certificates-java \
 && update-ca-certificates -f \
 && rm -rf /var/lib/apt/lists/* \
 && test "$(keytool -list -cacerts -storepass changeit 2>/dev/null | grep -c trustedCertEntry)" -gt 100

RUN useradd --system --no-create-home --shell /usr/sbin/nologin ivis
COPY --from=build /app/build/libs/ivis-backend.jar app.jar
USER ivis
ENV PORT=80
EXPOSE 80
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-XX:MaxRAMPercentage=75", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Xshare:auto", "-Xss512k", "-jar", "app.jar"]
