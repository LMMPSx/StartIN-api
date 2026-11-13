# ---------- Etapa 1: build do JAR com Maven + Java 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY StartIN-api-main/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY StartIN-api-main/src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa 2: imagem final enxuta (somente JRE) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=America/Sao_Paulo
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Duser.timezone=America/Sao_Paulo -Dfile.encoding=UTF-8"

COPY --from=build /app/target/startin-api-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
