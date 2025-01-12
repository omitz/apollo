//analytics that will be built with docker-compose, published to an ECR repo, and deployed to k8's with an autoscaler
def analytics = [
  'flask-apollo-processor',
  'virus-scanner',
  'speaker-recognition',
  'speaker-search',
  'speech-to-text',
  'file-type',
  'file-hash',
  'full-text-search',
  'sentiment-textblob',
  'object-detection-rabbit-consumer',
  'object-detection-vid',
  'named-entity-recognition',
  'named-entity-recognition-search',
  'landmark',
  'landmark-search',
  'ocr-keras',
  'ocr-easy',
  'ocr-tesseract',
  'facenet-search',
  'facenet-rabbit-consumer',
  'facenet-video-rabbit-consumer',
  'scene-places365',
]

void configureAWSCLI() {
  sh 'AWS_REGION="us-east-1"'
  sh 'aws configure set default.region us-east-1'
  sh 'aws configure set default.output json'
}
void loginEKS() {
  sh 'CLUSTER_NAME=dev-apollo-eks-cluster-2'
  sh 'aws eks update-kubeconfig --name dev-apollo-eks-cluster-2'
}

void loginECR() {
  sh 'AWS_REGION="us-east-1"'
  sh 'echo "Logging in to AWS ECR..."'
  sh 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com'
}

void deployEFSProvisioner() {
  sh 'kubectl apply -f ./infrastructure/helm/efs-k8s/'
}

void deployCantaloupe() {
  sh 'helm upgrade --wait --install cantaloupe-release ./infrastructure/helm/apollo-application -f ./infrastructure/helm/cantaloupe-values.yaml --set ingress.httpsCertificateArn=${IMAGE_SERVER_CERT_ARN}'
}

void deployMilvusChart() {
  sh 'helm upgrade --wait --install milvus-release ./infrastructure/helm/milvus-helm --set backendURL=${MYSQL_CONNECTION_STRING}'
}

void deployNeo4jChart() {
  sh 'helm upgrade --install neo4j-release ./infrastructure/helm/neo4j/ --set acceptLicenseAgreement=yes,neo4jPassword=${NEO4J_PASSWORD}'
}

void deployRabbitMQ() {
  sh 'helm upgrade --install rabbitmq-release ./infrastructure/helm/rabbitmq/ --set rabbitmq.username=${RABBITMQ_USER},rabbitmq.password=${RABBITMQ_PASSWORD},ingress.httpsCertificateArn=${RABBITMQ_CERT_ARN}'
}

void deployAwsAlbIngressController() {
  sh 'helm upgrade --wait --install aws-alb-ingress-controller-release ./infrastructure/helm/aws-alb-ingress-controller --set awsVPCID=${AWS_VPC_ID},awsRegion=us-east-1,clusterName=${CLUSTER_NAME}'
}

void deployK8sDashboard() {
  sh 'kubectl create namespace monitoring --dry-run=true -o yaml | kubectl apply -f -'
  sh 'helm upgrade --wait --install kubernetes-dashboard-release ./infrastructure/helm/kubernetes-dashboard -n monitoring --set ingress.httpsCertificateArn=${DASHBOARD_CERT_ARN}'
}

void deployMetricsServer() {
  sh 'helm upgrade --wait --install metrics-server-release ./infrastructure/helm/metrics-server/'
}

void deployExternalDNS() {
  sh 'helm upgrade --wait --install external-dns-release ./infrastructure/helm/external-dns --namespace kube-system --set provider=aws --set aws.zoneType=public'
}

void deployNeo4jSecret() {
  sh 'echo -n neo4j/${NEO4J_PASSWORD} | base64 -w 0 | xargs -I {} sed -i "s/{{auth}}/{}/" ./infrastructure/helm/secrets/neo4j-connection-secret.yaml'
  sh 'kubectl apply -f ./infrastructure/helm/secrets/neo4j-connection-secret.yaml'
}

void deployEKSClusterAutoscaler() {
  sh 'echo -n ${CLUSTER_NAME} | xargs -I {} sed -i "s/{{clustername}}/{}/" ./infrastructure/helm/cluster-autoscaler-autodiscover.yaml'
  sh 'echo -n us-east-1 | xargs -I {} sed -i "s/{{awsregion}}/{}/" ./infrastructure/helm/cluster-autoscaler-autodiscover.yaml'
  sh 'kubectl apply -f ./infrastructure/helm/cluster-autoscaler-autodiscover.yaml'
}

void deployCantaloupeSecret() {
  sh 'echo -n ${CANTALOUPE_USERNAME} | base64 -w 0 | xargs -I {} sed -i "s/{{username}}/{}/" ./infrastructure/helm/secrets/cantaloupe-connection-secret.yaml'
  sh 'echo -n ${CANTALOUPE_PASSWORD} | base64 -w 0 | xargs -I {} sed -i "s/{{password}}/{}/" ./infrastructure/helm/secrets/cantaloupe-connection-secret.yaml'
  sh 'echo -n ${S3_BUCKET_NAME} | base64 -w 0 | xargs -I {} sed -i "s/{{bucket}}/{}/" ./infrastructure/helm/secrets/cantaloupe-connection-secret.yaml'
  sh 'echo -n https://s3.us-east-1.amazonaws.com | base64 -w 0 | xargs -I {} sed -i "s/{{endpoint}}/{}/" ./infrastructure/helm/secrets/cantaloupe-connection-secret.yaml'
  sh 'kubectl apply -f ./infrastructure/helm/secrets/cantaloupe-connection-secret.yaml'
}

void deployRabbitmqSecret() {
  sh 'echo -n ${RABBITMQ_USER} | base64 -w 0 | xargs -I {} sed -i "s/{{user}}/{}/" ./infrastructure/helm/secrets/rabbitmq-connection-secret.yaml'
  sh 'echo -n ${RABBITMQ_PASSWORD} | base64 -w 0 | xargs -I {} sed -i "s/{{password}}/{}/" ./infrastructure/helm/secrets/rabbitmq-connection-secret.yaml'
  sh 'echo -n amqp://${RABBITMQ_USER}:${RABBITMQ_PASSWORD}@${RABBITMQ_HOST}:5672// | base64 -w 0 | xargs -I {} sed -i "s/{{uri}}/{}/" ./infrastructure/helm/secrets/rabbitmq-connection-secret.yaml'
  sh 'kubectl apply -f ./infrastructure/helm/secrets/rabbitmq-connection-secret.yaml'
}

void deployRabbitmqAutoscaler() {
  sh 'helm upgrade --wait --install rabbitmq-autoscaler-release ./infrastructure/helm/deployment-autoscaler --set image.repository=604877064041.dkr.ecr.us-east-1.amazonaws.com/pod-autoscaler,image.tag=v2,autoscale_kind=StatefulSet,autoscale_threshold=50,autoscale_max=100,autoscale_min=3,rabbitmq_host=${RABBITMQ_HOST},rabbitmq_user=${RABBITMQ_USER},rabbitmq_password=${RABBITMQ_PASSWORD},rabbitmq_queue="file_type_queue\\,virus_scanner_queue\\,face_queue\\,named_entity_recognition_queue\\,object_detection_queue\\,speech_to_text_queue\\,speaker_recognition_queue",autoscale_name=${RABBITMQ_HOST},serviceAccount.name=rabbitmq-autoscaler-deployment-service-account'
}

void deployPostgresSecret() {
  sh 'echo -n ${POSTGRES_HOST} | base64 -w 0 | xargs -I {} sed -i "s/{{host}}/{}/" ./infrastructure/helm/secrets/postgres-connection-secret.yaml'
  sh 'echo -n ${POSTGRES_PASSWORD} | base64 -w 0 | xargs -I {} sed -i "s/{{password}}/{}/" ./infrastructure/helm/secrets/postgres-connection-secret.yaml'
  sh 'kubectl apply -f ./infrastructure/helm/secrets/postgres-connection-secret.yaml'
}

void buildDocker(String imageName, String REPO_URI, String COMMIT_HASH) {
  loginECR()
  sh 'sudo chown -R ubuntu Command/milvus_apollo'
  sh 'docker-compose -f Command/docker-compose.yaml build ' + imageName
  sh 'docker tag command_' + imageName + ' ' + REPO_URI + imageName + ':latest'
  sh 'docker tag command_' + imageName + ' ' + REPO_URI + imageName + ':' + COMMIT_HASH
}

void pythonUnitTestAnalytic(String analytic) {
  if (analytic=='flask-apollo-processor') {
    sh 'env $(cat .test.env) docker-compose -f Command/docker-compose.yaml run -e JENKINS=True ' + analytic + ' python3 -m unittest'
  }
  else {
    sh 'env $(cat .test.env) docker-compose -f Command/docker-compose.yaml run -e JENKINS=True ' + analytic + ' python -m unittest'
  }
}

void publish(String imageName, String REPO_URI, String COMMIT_HASH) {
  sh 'docker push ' + REPO_URI + imageName + ':latest'
  sh 'docker push ' + REPO_URI + imageName + ':' + COMMIT_HASH
}

void deployAnalytic(String analyticName, String COMMIT_HASH) {
  stage('Deploy ' + analyticName) {
    stage('Deploy ' + analyticName + ' app') {
      deployHelmChart(analyticName, COMMIT_HASH)
    }
    if (analyticName != 'flask-apollo-processor') {
      stage('Deploy ' + analyticName + ' autoscaler') {
        deployAutoscaler(analyticName)
      }
    }
  }
}

void deployHelmChart(String chartName, String COMMIT_HASH) {
  retry(3) //deploys can fail do to too many charts being deployed consecutively
  {
    if(chartName == 'flask-apollo-processor') {
      sh 'helm upgrade --wait --install ' + chartName + '-release ./infrastructure/helm/apollo-application -f ./infrastructure/helm/' + chartName + '-values.yaml --set image.tag=' + COMMIT_HASH + ',rabbitmq_host=${RABBITMQ_HOST},ingress.httpsCertificateArn=${APOLLO_PROCESSOR_CERT_ARN}'
    }
    else {
      sh 'helm upgrade --wait --install ' + chartName + '-release ./infrastructure/helm/apollo-application -f ./infrastructure/helm/' + chartName + '-values.yaml --set image.tag=' + COMMIT_HASH + ',rabbitmq_host=${RABBITMQ_HOST}'
    }
  }
}

void deployAutoscaler(String serviceName) {
  retry(3){
    sh 'helm upgrade --wait --install ' + serviceName + '-autoscaler-release ./infrastructure/helm/deployment-autoscaler --set rabbitmq_host=${RABBITMQ_HOST},rabbitmq_user=${RABBITMQ_USER},rabbitmq_password=${RABBITMQ_PASSWORD},rabbitmq_queue=' + serviceName.replaceAll("-", "_") + '_queue,autoscale_name=' + serviceName + '-release-apollo-application,serviceAccount.name=' + serviceName + '-autoscaler-deployment-service-account'
  }
}

void cleanUpAnalytic(String imageName, String REPO_URI, String COMMIT_HASH) {
  sh 'docker image rm ' + REPO_URI + imageName + ':' + COMMIT_HASH
} 

void buildDockerCommandUI()
{
  loginECR()
  sh 'mv ./Command/command-ui/.prod.env ./Command/command-ui/.env'
  sh 'echo -n neo4j/${NEO4J_PASSWORD} | xargs -I {} sed -i "s|{{auth}}|{}|" ./Command/command-ui/.env'
  sh 'docker-compose -f Command/docker-compose.yaml build ui'
}

void deployCommandUI()
{
  sh 'docker-compose -f Command/docker-compose.yaml run ui npm run deploy'
}

//parallel service builds
def builders = [:]
for (x in analytics) {
  def analytic = x
  builders[analytic] = {
    node('jenkins-agent-ubuntu-ec2') {
      checkout scm 

      def AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
      def REPO_URI = sh(script: 'echo $(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com/', returnStdout: true).trim()
      def COMMIT_HASH = sh(script: 'git log -n 1 --pretty=format:\'%h\'', returnStdout: true).trim()
      def GIT_BRANCH = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()

      stage('setup env'){
        echo "Stage: setup env"
        environment {
          AWS_REGION = "us-east-1"
          CLUSTER_NAME = "dev-apollo-eks-cluster-2"
        }
      }
      //build all docker containers
      /*
      stage('Build Docker') {
        buildDocker(analytic, REPO_URI, COMMIT_HASH)  
      }

      //test microservices
      stage('Unit Test') {
        pythonUnitTestAnalytic(analytic)
      }
      */

      //master branch only, push microservices to ECR
      if (GIT_BRANCH == 'master') {

        stage("Publish Microservices") {
          configureAWSCLI()
          loginECR()
          publish(analytic, REPO_URI, COMMIT_HASH)
        }
      }

    }
  }

  builders['apollo-tests'] = {
    node('jenkins-agent-ubuntu-ec2') {
      checkout scm

      def AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
      def REPO_URI = sh(script: 'echo $(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com/', returnStdout: true).trim()
      def COMMIT_HASH = sh(script: 'git log -n 1 --pretty=format:\'%h\'', returnStdout: true).trim()
      def GIT_BRANCH = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()

      //build all docker containers
    /*
      stage('Build Docker') {
        //buildDocker('apollo-tests', REPO_URI, COMMIT_HASH)  
      }

      //test microservices
      stage('Unit Test') {
       // pythonUnitTestAnalytic('apollo-tests')
      }
    */
    }
  }
}

//parallel builders

node('jenkins-agent-ubuntu-ec2') {
  checkout scm 
  
  def AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
  def REPO_URI = sh(script: 'echo $(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com/', returnStdout: true).trim()
  def COMMIT_HASH = sh(script: 'git log -n 1 --pretty=format:\'%h\'', returnStdout: true).trim()
  def GIT_BRANCH = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
  
 //echo "WOLE WOLE WOLE  - it works branch=" + env.getEnvironment()
  if (true) {
    //master branch only, deploy connection secrets, deploy non-apollo services, and then apollo services to K8's cluster
    
    stage('setup env'){
      echo "Stage: setup env - Wole"
      environment {
        AWS_REGION = "us-east-1"
        CLUSTER_NAME = "dev-apollo-eks-cluster-2"
      }
    }
    stage('Set up') {

      echo "Stage: Set Up - Wole"
      configureAWSCLI()
      loginEKS()
    }

    //deploy connection secrets
    withCredentials([
      string(credentialsId: 'RABBITMQ_HOST', variable: 'RABBITMQ_HOST'), 
      string(credentialsId: 'RABBITMQ_USER', variable: 'RABBITMQ_USER'), 
      string(credentialsId: 'RABBITMQ_PASSWORD', variable: 'RABBITMQ_PASSWORD'),
      string(credentialsId: 'POSTGRES_PASSWORD', variable: 'POSTGRES_PASSWORD'),
      string(credentialsId: 'POSTGRES_HOST', variable: 'POSTGRES_HOST'),
      string(credentialsId: 'CANTALOUPE_USERNAME', variable: 'CANTALOUPE_USERNAME'),
      string(credentialsId: 'CANTALOUPE_PASSWORD', variable: 'CANTALOUPE_PASSWORD'),
      string(credentialsId: 'NEO4J_PASSWORD', variable: 'NEO4J_PASSWORD')
    ]) {
      stage("Deploy Secrets"){
        echo "Deploy Secrets - Wole"
        deployRabbitmqSecret()
        deployPostgresSecret()
        deployNeo4jSecret()
        deployMetricsServer()
      }
    }

    //deploy non-apollo services
    stage("Deploy First Services") {
      echo "Deploy First Services - Wole"

      deployEFSProvisioner()
      deployExternalDNS()
      deployK8sDashboard()
      deployAwsAlbIngressController()
      withCredentials([string(credentialsId: 'MYSQL_CONNECTION_STRING', variable: 'MYSQL_CONNECTION_STRING')]) {
        deployMilvusChart()
      }
      withCredentials([string(credentialsId: 'NEO4J_PASSWORD', variable: 'NEO4J_PASSWORD')]) {
        deployNeo4jChart()
      }
      withCredentials([
        string(credentialsId: 'RABBITMQ_HOST', variable: 'RABBITMQ_HOST'), 
        string(credentialsId: 'RABBITMQ_USER', variable: 'RABBITMQ_USER'), 
        string(credentialsId: 'RABBITMQ_PASSWORD', variable: 'RABBITMQ_PASSWORD')
      ]) {
        deployRabbitMQ()
        deployRabbitmqAutoscaler()
      }

      //Build & Deploy Command UI
      withCredentials([
        string(credentialsId: 'NEO4J_PASSWORD', variable: 'NEO4J_PASSWORD')
      ]) {
        buildDockerCommandUI()
        deployCommandUI()
      }
      echo "Deploy First Services - Wole"
      
      deployEKSClusterAutoscaler()
      deployCantaloupe()
    }


    withCredentials([
      string(credentialsId: 'RABBITMQ_HOST', variable: 'RABBITMQ_HOST'), 
      string(credentialsId: 'RABBITMQ_USER', variable: 'RABBITMQ_USER'), 
      string(credentialsId: 'RABBITMQ_PASSWORD', variable: 'RABBITMQ_PASSWORD')
    ]) {

      stage("Deploy Microservices") {
          
        def deployJobs = [:]
  
        analytics.each { analytic ->
          deployJobs[analytic] = {
            deployAnalytic(analytic, COMMIT_HASH)  
          }
        }

        parallel deployJobs
      }
    }
  }
}