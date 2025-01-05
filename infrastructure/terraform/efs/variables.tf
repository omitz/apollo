variable "environment" {
  type = string
}

variable "private-subnet-ids" {
  type = set(string)
}

variable "vpc-id" {
  type = string
}

variable "node-security-group-id" {
  type = string
}