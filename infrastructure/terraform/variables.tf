variable "aws_account_id" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "environment" {
  default = "dev"
  type = string
}

variable "docker_image_list" {
  type = set(string)
}

variable "min_nodes" {
  type = number
}

variable "max_nodes" {
  type = number
}

variable "desired_nodes" {
  type = number
}

variable "kubectl-aws-users" {
  type = set(string)
  default = []
}

variable "jenkins-ssl-certificate-arn" {
  type = string
}

variable "s3-bucket-name" {
  type = string
}

variable "ui-s3-bucket-name" {
  type = string
}