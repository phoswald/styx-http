$ mvn clean verify
$ java -jar target/simple-server.jar
$ sudo docker build -t simple-server .
$ sudo docker run -d --name my-container -p 8080:8080 -v ~/web:/usr/local/simple-server/content simple-server
