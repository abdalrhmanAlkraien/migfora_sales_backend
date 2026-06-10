FROM eclipse-temurin:25-jdk

LABEL version="1.0.0"
LABEL description="MIGFORA Sales application"
LABEL author="Abdalrhman ALkraien"

WORKDIR /app

RUN addgroup --system migfora && adduser --system --ingroup migfora migfora

COPY target/sales-0.0.1-SNAPSHOT.jar app.jar

RUN chown migfora:migfora app.jar

USER migfora

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-jar", "app.jar"]