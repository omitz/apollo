resource "aws_efs_file_system" "jenkins-efs" {
  creation_token = "jenkins-efs-secret-file-system-creation-token"

  tags = {
    Name = "${var.environment}-apollo-efs-jenkins-master"
  }

  lifecycle_policy {
    transition_to_ia = "AFTER_7_DAYS"
  }

}

resource "aws_efs_mount_target" "jenkins-efs" {
  for_each        = var.private-subnet-ids
  file_system_id  = aws_efs_file_system.jenkins-efs.id
  subnet_id       = each.value
  security_groups = [aws_security_group.jenkins-master-efs.id]
}