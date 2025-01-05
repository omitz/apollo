resource "aws_efs_file_system" "_" {
  creation_token = "my-product"

  tags = {
    Name = "apollo-${var.environment}-efs-milvus-data-mount"
  }

  lifecycle_policy {
    transition_to_ia = "AFTER_7_DAYS"
  }

}

resource "aws_efs_mount_target" "_" {
  for_each = var.private-subnet-ids
  file_system_id = aws_efs_file_system._.id
  subnet_id      = each.value
}


resource "aws_security_group" "_" {
  name        = "${var.environment}-efs-security-group"
  description = "Security group for efs"
  vpc_id      = var.vpc-id

  egress {
    from_port   = 0
    to_port = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-efs-security-group",
        )
}

resource "aws_security_group_rule" "efs-allow-nodes" {
  description              = "Allow nodes to communicate with efs"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  security_group_id        = aws_security_group._.id
  source_security_group_id = var.node-security-group-id
  type                     = "ingress"
}

resource "aws_security_group_rule" "node-ingress-efs" {
  description              = "Allow nodes to receive traffic from efs"
  from_port                = 0
  protocol                 = "-1"
  security_group_id        = var.node-security-group-id
  source_security_group_id = aws_security_group._.id
  to_port                  = 65535
  type                     = "ingress"
}