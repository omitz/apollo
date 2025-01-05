# Apollo Kubernetes/Helm deployment

## Requirements:
- kubectl configured in ~/.kube/config. To test, run `kubectl get nodes`
- helm version >= 3.0.0
  - https://helm.sh/docs/intro/install/

## Deploying the microservices
Each microservice can be deployed using `helm install {{name of release}} {{directory containing helm chart}}`. Helm release names must be unique across the cluster. Optionally, values can be set when deploying a release using the `--set` flag.

Most of the microservice charts depend on the rabbitmq chart already existing on the cluster and the rabbitmq chart depends on the aws-alb-ingress-controller chart already being deployed. Therefore, aws-alb-ingress-controller should be deployed first, followed by rabbitmq-ha.


An example for each microservice, running in the infrastructure/helm directory:
- aws-alb-ingress-controller: `helm upgrade --wait --install aws-alb-ingress-controller-release ./aws-alb-ingress-controller --set awsVPCID=vpc-0b650449a3d7d8735,awsRegion=us-east-1,clusterName=dev-apollo-eks-cluster`
  - the vpc id and clustername can be found by running `terraform output cluster-name` and `terraform output vpc-id` in the /infrastructure/terraform/ directory
- rabbitmq-ha: `helm upgrade --wait --install rabbitmq-release ./rabbitmq-ha/ --set rabbitmqUsername={{rabbitmq user}},rabbitmqPassword={{rabbitmq-password}},httpsCertificateArn=<< arn >>`
  - rabbitmq user and password values should be chosen
  - https certificate arn: https certificate can be generated with AWS Route53 and Certificate Manager
- kubernetes-dashboard: `helm upgrade --wait --install kubernetes-dashboard-release ./kubernetes-dashboard -n monitoring --set ingress.httpsCertificateArn=<< arn >>`
- metrics-server: `helm upgrade --wait --install metrics-server-release ./metrics-server/`
- The rest of the services (flask-apollo-processor and all data analytics) can be deployed with the pattern: 
  - `helm upgrade --wait --install << service name >>-release ./apollo-application -f ./<< service name >>-values.yaml --set image.tag=<< image tag >>,rabbitmq_host=rabbitmq-release-rabbitmq-ha`
  - ex: `helm upgrade --wait --install object-detection-vid-release ./apollo-application -f ./object-detection-vid-values.yaml --set image.tag=c06e9806,rabbitmq_host=rabbitmq-release`