# Eventer

Create events, invite friends and party hard!

This is a playground to get to know [ZIO](https://zio.dev/) and [React](https://reactjs.org/).


## Trying it Out

You can log in with email `example@example.org` and password `password`.
This account will be created automatically at application startup.


## Prerequisites

`docker`, `docker-compose`, `sbt` `npm` must be installed.


## Run Locally

Start the DB and reverse proxy (for SSL) with `docker-compose up`.  
Start the backend with `sbt run`.  
Start the frontend with `(cd frontend && npm start)`.  
Navigate to [https://localhost:3000](https://localhost:3000).

You might need to navigate to [https://localhost:9001](https://localhost:9001) once to accept the self-signed backend certificate.  
If you're not using Docker Desktop you might need to uncomment `network_mode: host` in the `./docker-compose.yaml`.


## Test Locally

Start the DB with `docker-compose up`.  
Test the backend with `sbt test`.  
Test the frontend with `(cd frontend && CI=true npm test)`.


## Features

- [x] list events
- [x] create events
- [ ] edit events
- [ ] view events
- [ ] invite internal guests (app-internal "friends")
- [ ] invite external guests (via email)
- [ ] user registration
- [x] user login
- [ ] guest login
- [ ] event locations (online + offline)