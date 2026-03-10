FROM eclipse-temurin:21-jre-alpine
ARG JAR_FILE=target/booking-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENV JAVA_OPTS=""
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
