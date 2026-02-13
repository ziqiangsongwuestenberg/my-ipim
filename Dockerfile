FROM eclipse-temurin:21-jre
WORKDIR /app

COPY build/libs/*.jar /app/app.jar
COPY build/otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

EXPOSE 8081

ENTRYPOINT ["java","-javaagent:/otel/opentelemetry-javaagent.jar","-Dotel.instrumentation.executors.enabled=true","-jar","/app/app.jar"]
