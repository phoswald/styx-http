$ mvn clean verify
$ sudo docker build -t sample-server .
$ sudo docker run -d --name my-container -p 8080:8080 sample-server
$ sudo docker run -d --name my-container -p 8080:8080 -v /home/philip/web:/usr/local/sample-server/content sample-server


$ docker run -d --name my-container -p 80:8080 -v /root/web:/usr/local/sample-server/content localhost:5000/sample-server

