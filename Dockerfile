# Unpack the built jar file and layer it
FROM azul/zulu-openjdk-alpine:17-jre as unpacker
COPY ./build/libs/*.jar /app/app.jar
WORKDIR /app
RUN java -Djarmode=layertools -jar app.jar extract

FROM azul/zulu-openjdk-alpine:17-jre
ENV JAVA_OPTS=""
WORKDIR /app

COPY --from=unpacker /app/dependencies/ ./
COPY --from=unpacker /app/spring-boot-loader/ ./
COPY --from=unpacker /app/snapshot-dependencies/ ./
COPY --from=unpacker /app/application/ ./

EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -Dserver.port=8080 -server org.springframework.boot.loader.JarLauncher
