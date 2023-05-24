FROM azul/zulu-openjdk-alpine:17-jre
ENV JAVA_OPTS=""
WORKDIR /app
COPY ./build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -Dserver.port=8080 -server -jar app.jar
