variable "environment" {
  type = string
}

variable "cluster-name" {
  type    = string
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

variable "region" {
  type = string
}

variable "ingress-security-group-ids" {
  type = set(string)
}