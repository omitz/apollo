output "kubeconfig" {
  value = local.kubeconfig
}

output "config_map_aws_auth" {
  value = local.config_map_aws_auth
}

output "aws-iam-role-node-arn" {
  value = aws_iam_role.node.arn
}

output "cluster-endpoint" {
  value = aws_eks_cluster.cluster.endpoint
}

output "cluster-certificate-authority" {
  value = aws_eks_cluster.cluster.certificate_authority.0.data
}

output "cluster-name" {
  value = var.cluster-name
}

output "vpc-id" {
  value = aws_vpc.vpc.id
}

output "public-subnet-ids" {
  value = aws_subnet.public_subnets.*.id
}

output "private-subnet-ids" {
  value = aws_subnet.private_subnets.*.id
}

output "eks-node-private-key-pem" {
  value = tls_private_key._.private_key_pem
}

output "node-security-group-id" {
  value = aws_security_group.node.id
}