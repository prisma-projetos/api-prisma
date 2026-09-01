# ---------- build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Baixa dependencias primeiro, para aproveitar cache de camada
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
ENV TZ=America/Fortaleza

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
