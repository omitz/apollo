output "jenkins-agent-iam-role-name" {
  value = aws_iam_role.agent-instance-role.name
}

output "jenkins-private-key-pem" {
  value = tls_private_key._.private_key_pem
}