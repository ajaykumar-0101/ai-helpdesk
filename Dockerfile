FROM eclipse-temurin:26-jre
WORKDIR /app
COPY deploy/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]