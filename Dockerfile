FROM azul/zulu-openjdk-alpine:17-jre
ENV JAVA_OPTS=""
WORKDIR /app
COPY ./build/libs/*.jar /app/app.jar

EXPOSE 8046

ENTRYPOINT exec java $JAVA_OPTS -server -jar app.jar
