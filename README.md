## Tests

To run Unit Tests:

```
sbt test
```

To run Integration Tests we need to run both `PostgreSQL` and `Redis`:


```
docker-compose up
sbt it:test
docker-compose down
```

## Build Docker image

```
sbt docker:publishLocal
```

Our image should now be built. We can check it by running the following command:

```
> docker images | grep net-worth-calculator
REPOSITORY                           TAG                 IMAGE ID            CREATED             SIZE
net-worth-calculator                 latest              646501a87362        2 seconds ago       138MB
```

To run our application using our Docker image, run the following command:

```
cd /app
docker-compose up
```