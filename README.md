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

## Run the frontend

The frontend is a React 19 application built with Vite and Tailwind CSS.

Install dependencies:

```
cd frontend
npm install
```

Run the development server:

```
npm run dev
```

Build for production:

```
npm run build
```

Preview the production build:

```
npm run preview
```

Lint the code:

```
npm run lint
```

## API Collection (Bruno)

A ready-to-use [Bruno](https://www.usebruno.com/) collection is included at `bruno/`. It covers all endpoints (auth, assets, transactions, health) and wires the JWT automatically.

How to use it:

1. Install Bruno if needed: `brew install bruno` (or download from the website).
2. Open Bruno → **Open Collection** → select the `bruno/` folder.
3. In the top-right dropdown, select the **Local** environment (points to `http://localhost:9000/v1`).
4. Run `Auth/Login` (or `Auth/Register`) first — the post-response script stores the returned JWT in the `token` environment variable automatically.
5. All other requests reference `{{token}}` for authorization and will work without further setup.

Some requests (e.g. `Delete Asset`, `Transactions by Asset Id`, `Transactions by Asset Type`) define pre-request variables (`assetId`, `transactionId`, `assetType`) you can edit inline to target different records.

See `API_REFERENCE.md` for a full description of each endpoint, request bodies, and response shapes.

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