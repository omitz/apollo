To build a milvus docker container that analytics containers (in this example, `face`) can connect to (for local testing only) :

Follow instructions here https://milvus.io/docs/v0.7.0/guides/get_started/install_milvus/cpu_milvus_docker.md to create a `conf` directory and `wget` config files. Your tree inside milvus_apollo should ultimately look like:

```
milvus_apollo$ tree
├── milvus
│   ├── conf
│   │   ├── server_config.yaml
│   │   └── server_config.yaml.1
```

- If testing a local container, add `MILVUS_HOST=milvus` to your .env file
    
    ```
    docker-compose build milvus <analytic e.g. face>
    docker-compose up -d milvus <analytic e.g. face>
    ```
    

- Alternatively, if testing a local script (ie outside of a container, so you can easily use a debugger, avoid the time-sink that is building a container, etc.)

    Get the IP address for your milvus container. You can do this by running
    
    `docker inspect --format='{{.Name}} {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $(docker ps -q)`
    
    Add `MILVUS_HOST=<that IP address>` to your environment variables.
    
    The same principal goes for POSTGRES. (set POSTGRES_HOST to the IP address of the container and set POSTGRES_PASSWORD to secretpassword (literally))

