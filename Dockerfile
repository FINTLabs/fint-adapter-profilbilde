FROM eclipse-temurin:8-jdk-noble AS builder
WORKDIR /home/gradle
COPY . .
ARG apiVersion
RUN sh ./gradlew --no-daemon -PapiVersion=${apiVersion} build

FROM eclipse-temurin:8-noble
ENV JAVA_TOOL_OPTIONS=-XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-adapter-profilbilde-*.jar /data/fint-adapter-profilbilde.jar
VOLUME /images
CMD ["java", "-jar", "/data/fint-adapter-profilbilde.jar"]
