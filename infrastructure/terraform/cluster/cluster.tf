resource "aws_eks_cluster" "cluster" {
  name            = var.cluster-name
  role_arn        = aws_iam_role.cluster-iam-role.arn

  vpc_config {
    security_group_ids = [aws_security_group.cluster.id]
    subnet_ids         = concat(aws_subnet.private_subnets.*.id, aws_subnet.public_subnets.*.id)
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster-AmazonEKSClusterPolicy,
    aws_iam_role_policy_attachment.cluster-AmazonEKSServicePolicy,
  ]
}