#### To run tests

    # From apollo/Command
    docker-compose build flask-apollo-processor
    docker-compose up -d flask-apollo-processor
    docker-compose run flask-apollo-processor python3 -m unittest

#### Potential Issues

##### 504 Gateway Time-out

It's possible for a service to work locally, but not when deployed because the connection times out before the result is returned. In the past, this has been the case for uploading a landmark image when there are many images in the landmark database.  

The solution is to either optimize the analytic for speed or increase the load balancer's timeout. The load balancer's timeout is defined in `infrastructure/helm/flask-apollo-processor-values.yaml` by 

    ingress:
      ...
      annotations: 
        ...
        alb.ingress.kubernetes.io/load-balancer-attributes: idle_timeout.timeout_seconds=300