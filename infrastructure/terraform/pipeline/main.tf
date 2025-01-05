
#random number to add to s3 bucket name, all s3 bucket names must be globally unique
resource "random_id" "random_s3" {
  byte_length = 2
}

resource "aws_codebuild_project" "code-project-build-docker-image" {
  for_each = var.docker_image_list
  name          = "codebuild-project-build-${each.value}"
  description   = "builds ${each.value} docker image and pushes it to ECR"
  build_timeout = "30"
  service_role  = "${aws_iam_role.codebuild-project-build-docker-service-role.arn}"

  artifacts {
    type = "NO_ARTIFACTS"
  }

  cache {
    type     = "LOCAL"
    modes    = ["LOCAL_DOCKER_LAYER_CACHE"]
  }

  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:1.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode = true

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = var.aws_account_id
    }
  }

  logs_config {
    cloudwatch_logs {
      group_name = "apollo-codebuild"
      stream_name = "build-${each.value}"
    }
  }

  source {
    type            = "NO_SOURCE"
    buildspec = <<EOF
version: 0.2

phases:
  pre_build:
    commands:
      - echo Logging into Amazon ECR...
      - $(aws ecr get-login --no-include-email --region $AWS_DEFAULT_REGION)
      - COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)
  build:
    commands:
      - echo Build started on `date`
      - echo Build docker images
      - IMAGE_REPO_NAME=${each.value}
      - REPO_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME
      - echo -------------------- $IMAGE_REPO_NAME --------------------
      - echo Building \'$IMAGE_REPO_NAME\' image
      - cp -r Command/utils/ Command/$IMAGE_REPO_NAME/utils/
      - docker build -t $REPO_URI:latest -t $REPO_URI:$COMMIT_HASH Command/$IMAGE_REPO_NAME/
      - echo Tagging \'$IMAGE_REPO_NAME\' image
      - echo Build completed on `date`
      - echo Pushing \'$IMAGE_REPO_NAME\'...
      - docker push $REPO_URI:latest
      - docker push $REPO_URI:$COMMIT_HASH
EOF
  }
}

#speech to text images must be configured slightly differently
resource "aws_codebuild_project" "speech-to-text-code-project-build-docker-image" {
  for_each = var.speech_to_text_docker_image_list
  name          = "codebuild-project-build-${each.value}"
  description   = "builds ${each.value} docker image and pushes it to ECR"
  build_timeout = "60"
  service_role  = "${aws_iam_role.codebuild-project-build-docker-service-role.arn}"

  artifacts {
    type = "NO_ARTIFACTS"
  }

  cache {
    type     = "LOCAL"
    modes    = ["LOCAL_DOCKER_LAYER_CACHE"]
  }

  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:1.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode = true

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = var.aws_account_id
    }
  }

  logs_config {
    cloudwatch_logs {
      group_name = "apollo-codebuild"
      stream_name = "build-${each.value}"
    }
  }

  source {
    type            = "NO_SOURCE"
    buildspec = <<EOF
version: 0.2

phases:
  pre_build:
    commands:
      - echo Logging into Amazon ECR...
      - $(aws ecr get-login --no-include-email --region $AWS_DEFAULT_REGION)
      - COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)
  build:
    commands:
      - echo Build started on `date`
      - echo Build docker images
      - IMAGE_REPO_NAME=${each.value}
      - REPO_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME
      - echo -------------------- $IMAGE_REPO_NAME --------------------
      - echo Building \'$IMAGE_REPO_NAME\' image
      - cp -r Command/utils/ Command/speech_to_text/docker-$IMAGE_REPO_NAME/utils/
      - docker build -t $REPO_URI:latest -t $REPO_URI:$COMMIT_HASH Command/speech_to_text/docker-$IMAGE_REPO_NAME/
      - echo Tagging \'$IMAGE_REPO_NAME\' image
      - echo Build completed on `date`
      - echo Pushing \'$IMAGE_REPO_NAME\'...
      - docker push $REPO_URI:latest
      - docker push $REPO_URI:$COMMIT_HASH
EOF
  }
}

resource "aws_codepipeline_webhook" "_" {
  name            = "test-webhook-github-bar"
  authentication  = "GITHUB_HMAC"
  target_action   = "Source"
  target_pipeline = aws_codepipeline.codepipeline-apollo-dev.name

  authentication_configuration {
    secret_token = var.github_webhook_secret
  }

  filter {
    json_path    = "$.ref"
    match_equals = "refs/heads/{Branch}"
  }
}

resource "github_repository_webhook" "_" {
  repository = "apollo"

  configuration {
    url          = "${aws_codepipeline_webhook._.url}"
    content_type = "json"
    insecure_ssl = false
    secret       = var.github_webhook_secret
  }

  events = ["push"]
}

resource "aws_s3_bucket" "codepipeline_bucket" {
  bucket = "apollo-codepipeline-${var.aws_region}-${random_id.random_s3.dec}"
  acl = "private"
  force_destroy = true
}

resource "aws_codepipeline" "codepipeline-apollo-dev" {
  name = "codepipeline-apollo-dev"
  role_arn = aws_iam_role.codepipeline-service-role.arn

  artifact_store {
    location = "${aws_s3_bucket.codepipeline_bucket.bucket}"
    type     = "S3"
  }

  stage {
      name = "Source"

      action {
        name             = "Source"
        category         = "Source"
        owner            = "ThirdParty"
        provider         = "GitHub"
        version          = "1"
        output_artifacts = ["source_output"]

        configuration = {
          Owner  = "NextCenturyCorporation"
          Repo   = "apollo"
          Branch = "master"
          OAuthToken           = var.github_token
          PollForSourceChanges = "false"
        }
      }
    }

    stage {
      name = "Build"

      dynamic "action" {
        for_each = var.docker_image_list
        content {
          name             = "Build-${action.value}"
          category         = "Build"
          owner            = "AWS"
          provider         = "CodeBuild"
          input_artifacts  = ["source_output"]
          version          = "1"

          configuration = {
            ProjectName = "codebuild-project-build-${action.value}"
          }
        }
      }

      dynamic "action" {
        for_each = var.speech_to_text_docker_image_list
        content {
          name             = "Build-${action.value}"
          category         = "Build"
          owner            = "AWS"
          provider         = "CodeBuild"
          input_artifacts  = ["source_output"]
          version          = "1"

          configuration = {
            ProjectName = "codebuild-project-build-${action.value}"
          }
        }
      }
    }
}