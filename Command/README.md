# Running the Command server locally

## Requirements:
- install docker
- install docker-compose
  - https://docs.docker.com/compose/install/

## Building and running the microservices
The docker-compose microservices require some environment variables to provide AWS credentials and to connect to eachother. Docker-compose can read these environment variables from a file named `.env` in the `Command/` directory and following the format: 

```
AWS_ACCESS_KEY_ID=....
AWS_SECRET_ACCESS_KEY=...
BUCKET_NAME=apollo-source-data
RABBITMQ_HOST=rabbitmq
POSTGRES_HOST=postgres
POSTGRES_PASSWORD=secretpassword
POSTGRES_USER=postgres
DEBUG=True
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
MILVUS_HOST=milvus
NEO4J_HOST=neo4j
NEO4J_AUTH=neo4j/neo4j-password  #this should be in the format username/password
ENDPOINT_ADMIN_USERNAME=admin
ENDPOINT_ADMIN_SECRET=cantaloupe-password
NER_SEARCH_HOST=named-entity-recognition-search
```

Then from the directory `apollo/Command`, run `docker-compose build` and `docker-compose up -d` to run the services in the background.
Apollo command server should be exposed at localhost:8080 and rabbitmq management console should be accessible at localhost:15672 with username:password guest:guest.

## Useful commands for developing/debugging
- `docker-compose ps` to check status of services
- `docker-compose logs <<service name>>` to get logs of a service
- `docker-compose build <<service name>>` to rebuild only one service
- `docker-compose up <<service name>>` to restart only one service

## Using the REST API

Apollo's primary REST API (`flask-apollo-processor`) is documentated via Swagger. The `flask-apollo-processor` service exposes the Swagger doc at `localhost:8080/swagger/`. The Swagger documentation provides an easy way to see all the analytics included in Apollo. It also provides examples of how to run jobs (i.e. run inference) and query the results of previous jobs.

#### Getting started with Swagger

* Navigate to `localhost:8080/swagger/` 
* Click the tab for `/login/`
* Click `Try it out`
* (Optionally) Change the `username` and `password` to your own.
* Click `Execute`. Swagger will generate a `curl` command for the request.
* Copy the authorization token (the very long string following `Authorization Bearer`) from the `curl` command
* At the top of the Swagger page, click `Authorize`
* Paste the authorization token and click `Authorize`.

All further requests sent via Swagger will include the header with the authorization token.

## Run unit tests

    # From apollo/Command
    docker-compose run <analytic> python -m unittest
    # e.g.
    docker-compose run file-hash python -m unittest

