# 1) Build the React app
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Build the Spring Boot jar, with the React build inside it
FROM eclipse-temurin:26-jdk AS backend
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q dependency:go-offline
COPY src src
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN ./mvnw -q -DskipTests package

# 3) Run it
FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]