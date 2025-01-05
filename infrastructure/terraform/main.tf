terraform {
  backend "s3" {
    bucket         = "apollo-terraform-state-4693"
    region         = "us-east-1"
    key            = "global/s3/terraform.tfstate"
    dynamodb_table = "terraform-locks"
    encrypt        = true
  }
  required_providers {
    aws = "~> 2.68"
  }
}

provider "aws" {
  profile    = "default"
  region     = var.aws_region
}

/*provider "github" {
  token        = var.github_token
  organization = "NextCenturyCorporation"
}

module "pipeline" {
  source = "./pipeline"

  aws_region = var.aws_region
  aws_account_id = var.aws_account_id
  github_webhook_secret = var.github_webhook_secret
  github_token = var.github_token
  docker_image_list = var.docker_image_list
  speech_to_text_docker_image_list = var.speech_to_text_docker_image_list
}*/

module "jenkins-ec2" {
  source = "./jenkins-ec2"
  
  environment = var.environment
  aws-region = var.aws_region
  aws-account-id = var.aws_account_id
  public-subnet-ids = module.cluster.public-subnet-ids
  private-subnet-ids = module.cluster.private-subnet-ids
  cluster-name = module.cluster.cluster-name
  vpc-id = module.cluster.vpc-id
  certificate-arn = var.jenkins-ssl-certificate-arn
  s3-bucket-name = var.s3-bucket-name
  ui-s3-bucket-name = var.ui-s3-bucket-name
}

module "ecr" {
  source = "./ecr"

  docker_image_list = var.docker_image_list
}

module "postgres_rds" {
  source = "./rds"

  environment = var.environment
  aws_region = var.aws_region
  aws_account_id = var.aws_account_id
  subnet-ids = module.cluster.private-subnet-ids
  vpc-id = module.cluster.vpc-id
  node-security-group-id = module.cluster.node-security-group-id
  name = "${var.environment}ApolloPostgresRDS"
}


module "mysql_rds" {
  source = "./rds"

  environment = var.environment
  aws_region = var.aws_region
  aws_account_id = var.aws_account_id
  subnet-ids = module.cluster.private-subnet-ids
  vpc-id = module.cluster.vpc-id
  node-security-group-id = module.cluster.node-security-group-id
  allocated_storage = 5
  max_allocated_storage = 20
  engine = "mysql"
  engine_version = "5.7"
  name = "${var.environment}ApolloMysqlRDS"
  admin_username = "admin"
  parameter_group_name = "mysql5.7"
  port_number = 3306
}

module "efs" {
  source                  = "./efs"

  environment             = var.environment
  private-subnet-ids      =  module.cluster.private-subnet-ids
  vpc-id                  = module.cluster.vpc-id
  node-security-group-id  = module.cluster.node-security-group-id
}

module "cluster" {
  source                      = "./cluster"

  environment                 = var.environment
  cluster-name                = "${var.environment}-apollo-eks-cluster"
  desired_nodes               = var.desired_nodes
  min_nodes                   = var.min_nodes
  max_nodes                   = var.max_nodes
  region                      = var.aws_region
  ingress-security-group-ids  = [module.efs.efs-security-group-id]
}


#kubernetes module relies on output from cluster module (kubeconfig)
resource "local_file" "kubeconfig" {
  content     = module.cluster.kubeconfig
  filename = "${path.module}/kubeconfig"
}

resource "local_file" "aws-auth-config-map" {
  content = module.cluster.config_map_aws_auth
  filename = "${path.module}/aws-auth.yaml"
}

data "external" "aws_iam_authenticator" {
  program = ["sh", "-c", "aws-iam-authenticator token -i ${module.cluster.cluster-name} | jq -r -c .status"]
}

provider "kubernetes" {
  host = module.cluster.cluster-endpoint
  cluster_ca_certificate = base64decode(module.cluster.cluster-certificate-authority)
  token = data.external.aws_iam_authenticator.result.token
  load_config_file = false
}

module "kubernetes" {
  source = "./kubernetes"

  aws-iam-role-node-arn = module.cluster.aws-iam-role-node-arn
  users = var.kubectl-aws-users
  aws-account-id = var.aws_account_id
  jenkins-iam-role-name = module.jenkins-ec2.jenkins-agent-iam-role-name

}

resource "local_file" "eks-node-private-key-pem" {
  content     = module.cluster.eks-node-private-key-pem
  filename    = "${var.environment}-apollo-eks-cluster-key.pem"
}

resource "local_file" "jenkins-private-key-pem" {
  content     = module.jenkins-ec2.jenkins-private-key-pem
  filename    = "${var.environment}-apollo-jenkins-key.pem"
}