variable "aws_account_id" {
    type = string
}

variable "aws_region" {
    type = string
}

variable "environment" {
    type = string
}

variable "subnet-ids" {
    type = list(string)
}

variable "vpc-id" {
    type = string
}

variable "node-security-group-id" {
    type = string
}

variable "allocated_storage" {
    type = number
    default = 20
}

variable "max_allocated_storage" {
    type = number
    default = 100
}

variable "engine" {
    type = string
    default = "postgres"
}

variable "engine_version" {
    type = string
    default = "11.8"
}

variable "instance_class" {
    type = string
    default = "db.t3.small"
}

variable "name" {
    type = string
}

variable "admin_username" {
    type = string
    default = "postgres"
}

variable "parameter_group_name" {
    type = string
    default = "postgres11"
}

variable "port_number" {
    type = number
    default = 5432
}