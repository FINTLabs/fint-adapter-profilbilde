FROM gradle:8.11.1-jdk25-alpine as builder
USER root
COPY . .
ARG apiVersion
RUN gradle --no-daemon -PapiVersion=${apiVersion} build

FROM gcr.io/distroless/java25-debian12
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-adapter-profilbilde-*.jar /data/fint-adapter-profilbilde.jar
VOLUME /images
CMD ["/data/fint-adapter-profilbilde.jar"]
