## Net Worth Calculator
Welcome to Net Worth Calculator! The application is written in Scala 3 using the Tagless Final Pattern and pure functional libraries like Cats, Cats Effect, Http4s and Doobie.

It uses PostgreSQL as a relational database and Redis as an in-memory data structure store.

## Run the application using sbt

Run both `PostgreSQL` and `Redis`:

```
docker-compose up -d
```

Run the application

```
sbt run
```

## Build Docker image

```
sbt docker:publishLocal
```

To check the built image:

```
> docker images | grep net-worth-calculator
REPOSITORY                           TAG                 IMAGE ID            CREATED             SIZE
net-worth-calculator                 latest              646501a87362        2 seconds ago       138MB
```

## Run the application using docker compose

```
cd /app
docker-compose up
```

## Access Adminer
Using your browser of choice, connect to localhost:8080 and input the following information:

```
System: PostgreSQL
Server: postgres
Username: postgres
Password: secret
Database: networth
```

## Troubleshooting

Within the container:

* netstat -tulpn
* wget

docker run -d -p 9000:9000 --env NWC_APP_ENV=test net-worth-calculator

Postgres docker container:

* docker exec
* /var/lib/pgsql/data
* psql --host=postgres --username=postgres --dbname=networth
* \dt (list of tables)

