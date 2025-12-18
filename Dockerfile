FROM eclipse-temurin:21-jdk
VOLUME /tmp
EXPOSE 8080
COPY target/dynamic-platform-1.0.0.jar dynamic-platform.jar
ENTRYPOINT ["java","-jar","/dynamic-platform.jar"]