FROM openjdk:8-jre-alpine
COPY target/simple-server.jar /usr/local/simple-server/
COPY target/lib               /usr/local/simple-server/lib
EXPOSE 8080
WORKDIR /usr/local/simple-server
CMD exec java -Dcontent=/usr/local/simple-server/content -jar simple-server.jar
