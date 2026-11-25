FROM eclipse-temurin:26-jre
WORKDIR /app
ADD https://github.com/ajaykumar-0101/ai-helpdesk/releases/download/v2/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
EXPOSE 8083

CMD ["java", "-jar", "app.jar"]