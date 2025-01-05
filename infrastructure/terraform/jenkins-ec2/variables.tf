variable "aws-account-id" {
    type = string
}

variable "aws-region" {
    type = string
}

variable "cluster-name" {
    type = string
}

variable "environment" {
    type = string
}

variable "private-subnet-ids" {
    type = set(string)
}

variable "public-subnet-ids" {
    type = set(string)
}

variable "vpc-id" {
    type = string
}

variable "certificate-arn" {
    type = string
}

variable "s3-bucket-name" {
    type = string
}

variable "ui-s3-bucket-name" {
    type = string
}