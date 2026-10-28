FROM gradle:9.1.0-jdk25 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home --shell /usr/sbin/nologin ivis
COPY --from=build /app/build/libs/ivis-backend.jar app.jar
USER ivis
EXPOSE 8081
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-XX:MaxRAMPercentage=75", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Xshare:auto", "-Xss512k", "-jar", "app.jar"]
