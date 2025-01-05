# Apollo Infrastructure

## Requirements:
- AWS account with correct permissions
- Admin access to Apollo github repository (to create webhook)
- Terraform >= v0.12 
  - https://www.terraform.io/downloads.html
- kubectl
  - https://kubernetes.io/docs/tasks/tools/install-kubectl/
- helm
  - https://helm.sh/docs/intro/install/
- jq
  - https://stedolan.github.io/jq/manual/
- aws-iam-authenticator
  - https://docs.aws.amazon.com/eks/latest/userguide/install-aws-iam-authenticator.html

## Deploying the Infrastructure
Create a github personal access token with permissions to create github webhooks.
  - https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line
  - required permissions: "repo" and "admin:repo_hook"

In the `infrastructure/terraform/terraform-backend` directory, run `terraform init && terraform apply -auto-approve`.
  - This will create an S3 bucket to store the terraform state for the main infrastructure deployment

In the `infrastructure/terraform/` directory, create a file called `terraform.tfvars` with the following structure:

```
aws_account_id = "<< apollo aws account id >>"
aws_region = "us-east-1"
environment = "dev"
jenkins-ssl-certificate-arn = "<< jenkins ssl certificate arn created with AWS route53 and Certificate Manager >>"
```

Then run: 
- `terraform init`
- `terraform plan -out <<name of plan>>`
- `terraform apply <<name of plan>>`
- `cp kubeconfig ~/.kube/config`

The name of plan can be anything

## Tearing Down the Infrastructure
In the `infrastructure/terraform/` run `terraform destroy`

