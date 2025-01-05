variable "aws_account_id" {
    type = string
}

variable "aws_region" {
    type = string
}

variable "github_token" {
    type = string
}
variable "github_webhook_secret" {
    type = string
}

variable "docker_image_list" {
    type = set(string)
}

#speech to text images must be configured slightly differently
variable "speech_to_text_docker_image_list" {
    type = set(string)
}