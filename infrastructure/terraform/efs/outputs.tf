output "efs-id" {
  value = aws_efs_file_system._.id
}

output "efs-security-group-id" {
  value = aws_security_group._.id
}