resource "aws_ecr_repository" "ecr-repository" {
  for_each = var.docker_image_list
  name                 = each.value
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }
}