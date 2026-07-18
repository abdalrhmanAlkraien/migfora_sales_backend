FROM eclipse-temurin:25-jdk

LABEL version="1.0.0"
LABEL description="MIGFORA Sales application"
LABEL author="Abdalrhman ALkraien"

WORKDIR /app

# Install recon tools from apt
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    dnsutils \
    nmap \
    whois \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install subfinder from GitHub releases
RUN curl -sSL https://github.com/projectdiscovery/subfinder/releases/download/v2.6.6/subfinder_2.6.6_linux_amd64.zip \
    -o subfinder.zip \
    && unzip subfinder.zip subfinder \
    && mv subfinder /usr/local/bin/subfinder \
    && chmod +x /usr/local/bin/subfinder \
    && rm subfinder.zip

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
  "--enable-preview", \
  "-jar", "app.jar"]