FROM gradle:7.6.1-jdk17 AS build
ARG DB_PORT
ENV DB_PORT=$DB_PORT
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle flywayclean flywaymigrate bootjar --no-daemon


FROM azul/zulu-openjdk-alpine:17-jre
ENV JAVA_OPTS=""
WORKDIR /app
#COPY ./build/libs/*.jar /app/app.jar
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -Dserver.port=8080 -server -jar app.jar
