FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
# Copy Firebase service account credentials
COPY service-account.json service-account.json
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]