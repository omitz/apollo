variable "aws-iam-role-node-arn" {
    type = string
}

variable "aws-account-id" {
  type = number
}

variable "jenkins-iam-role-name" {
  type = string
}

variable "users" {
  type = set(string)
  default = []
}