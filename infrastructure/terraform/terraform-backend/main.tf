#S3 bucket to store terraform state
provider "aws" {
    region = "us-east-1"
}

resource "aws_s3_bucket" "terraform_state_bucket" {
  bucket = "apollo-terraform-state-4693"

  
  versioning {
    enabled = true
  }

  server_side_encryption_configuration {
    #tf state is stored in plaintext, encryption is necessary
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }

  lifecycle {
      prevent_destroy = true
  }
}

resource "aws_dynamodb_table" "terraform_locks" {
  name         = "terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"
  attribute {
    name = "LockID"
    type = "S"
  }
}