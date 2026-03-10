FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=target/bookmyshow-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS=""

EXPOSE 8761

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]

