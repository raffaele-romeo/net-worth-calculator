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

# Troubleshooting
Within the container:

* netstat -tulpn
* wget

docker run -d -p 8080:8080 --env NWC_APP_ENV=test net-worth-calculator

Postgres docker container:
* docker exec
* /var/lib/pgsql/data
* psql --host=postgres --username=postgres --dbname=networth
* \dt (list of tables)

