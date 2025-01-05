resource "aws_iam_instance_profile" "master-instance-profile" {
  name = "master-instance-profile"
  role = aws_iam_role.master-instance-role.name
}

resource "aws_iam_instance_profile" "agent-instance-profile" {
  name = "agent-instance-profile"
  role = aws_iam_role.agent-instance-role.name
}

resource "aws_iam_role" "agent-instance-role" {
  name = "${var.environment}-apollo-jenkins-agent-instance-role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role" "master-instance-role" {
  name = "${var.environment}-apollo-jenkins-master-instance-role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_policy" "eks-policy" {
  name = "${var.environment}-apollo-jenkins-agent-eks-access-policy"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
          "eks:DescribeNodegroup",
          "eks:ListNodegroups",
          "eks:ListTagsForResource",
          "eks:DescribeUpdate",
          "eks:ListUpdates",
          "eks:DescribeCluster"
      ],
      "Resource": "arn:aws:eks:${var.aws-region}:${var.aws-account-id}:cluster/${var.cluster-name}"
    },
    {
      "Effect": "Allow",
      "Action": "eks:ListClusters",
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_policy" "ecr-policy" {
  name = "${var.environment}-apollo-jenkins-agent-ecr-access-policy"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:PutImageTagMutability",
        "ecr:StartImageScan",
        "ecr:ListTagsForResource",
        "ecr:UploadLayerPart",
        "ecr:BatchDeleteImage",
        "ecr:ListImages",
        "ecr:DeleteRepository",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeRepositories",
        "ecr:DeleteRepositoryPolicy",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetLifecyclePolicy",
        "ecr:PutLifecyclePolicy",
        "ecr:DescribeImageScanFindings",
        "ecr:GetLifecyclePolicyPreview",
        "ecr:CreateRepository",
        "ecr:PutImageScanningConfiguration",
        "ecr:GetDownloadUrlForLayer",
        "ecr:GetAuthorizationToken",
        "ecr:DeleteLifecyclePolicy",
        "ecr:PutImage",
        "ecr:BatchGetImage",
        "ecr:DescribeImages",
        "ecr:StartLifecyclePolicyPreview",
        "ecr:InitiateLayerUpload",
        "ecr:GetRepositoryPolicy"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_policy" "s3-policy" {
  name = "${var.environment}-apollo-jenkins-agent-s3-access-policy"
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::${var.s3-bucket-name}"
        },
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::${var.ui-s3-bucket-name}"
        }
    ]
}
EOF
}

resource "aws_iam_policy" "ec2-policy" {
  name = "${var.environment}-apollo-jenkins-ec2-policy"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeSpotInstanceRequests",
        "ec2:CancelSpotInstanceRequests",
        "ec2:GetConsoleOutput",
        "ec2:RequestSpotInstances",
        "ec2:RunInstances",
        "ec2:StartInstances",
        "ec2:StopInstances",
        "ec2:TerminateInstances",
        "ec2:CreateTags",
        "ec2:DeleteTags",
        "ec2:DescribeInstances",
        "ec2:DescribeKeyPairs",
        "ec2:DescribeRegions",
        "ec2:DescribeImages",
        "ec2:DescribeAvailabilityZones",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeSubnets",
        "iam:ListInstanceProfilesForRole",
        "iam:PassRole",
        "ec2:GetPasswordData"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_policy" "efs-policy" {
  name = "${var.environment}-apollo-jenkins-efs-access-policy"
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
              "elasticfilesystem:*"
            ],
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "jenkins-master-ec2-role-attachment" {
  role      = aws_iam_role.master-instance-role.name
  policy_arn = aws_iam_policy.ec2-policy.arn
}

resource "aws_iam_role_policy_attachment" "jenkins-master-efs-role-attachment" {
  role      = aws_iam_role.master-instance-role.name
  policy_arn = aws_iam_policy.efs-policy.arn
}

resource "aws_iam_role_policy_attachment" "jenkins-agent-eks-role-attachment" {
  role      = aws_iam_role.agent-instance-role.name
  policy_arn = aws_iam_policy.eks-policy.arn
}

resource "aws_iam_role_policy_attachment" "jenkins-agent-ecr-role-attachment" {
  role      = aws_iam_role.agent-instance-role.name
  policy_arn = aws_iam_policy.ecr-policy.arn
}

resource "aws_iam_role_policy_attachment" "jenkins-agent-s3-role-attachment" {
  role      = aws_iam_role.agent-instance-role.name
  policy_arn = aws_iam_policy.s3-policy.arn
}