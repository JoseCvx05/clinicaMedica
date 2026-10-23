# =====================================================
# SANTOS CLÍNICA MÉDICA
# DOCKER - JAVA 17 + SPRING BOOT
# =====================================================

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn -B -Dmaven.test.skip=true clean package

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=America/Guatemala

COPY --from=build /app/target/clinicamedica-0.0.1-SNAPSHOT.jar app.jar

# =====================================================
# DIRECTORIO DE DOCUMENTOS
# Permisos para el usuario 10001
# =====================================================

RUN mkdir -p /app/uploads/citas \
    && chown -R 10001:0 /app/uploads

EXPOSE 8080

USER 10001

ENTRYPOINT ["java", "-jar", "app.jar"]