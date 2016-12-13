FROM openjdk:8-jre-alpine
COPY sample-server/target/sample-server.jar /usr/local/sample-server/
COPY sample-server/target/lib               /usr/local/sample-server/lib
EXPOSE 8080
WORKDIR /usr/local/sample-server
CMD exec java -Dcontent=/usr/local/sample-server/content -jar sample-server.jar

