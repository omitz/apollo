resource "aws_db_instance" "_" {
  allocated_storage    = var.allocated_storage
  max_allocated_storage= var.max_allocated_storage
  storage_type         = "gp2"
  engine               = var.engine
  engine_version       = var.engine_version
  instance_class       = var.instance_class
  name                 = var.name
  username             = var.admin_username
  password             = random_password.password.result
  parameter_group_name = aws_db_parameter_group._.name
  deletion_protection  = true
  iam_database_authentication_enabled = true
  multi_az = true
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name = aws_db_subnet_group._.name
}

resource "random_password" "password" {
  length = 32
  special = true
  override_special = "_%@"
}

resource "aws_db_subnet_group" "_" {
  name       = "${var.environment}-${var.engine}-apollo-database-subnet-group"
  subnet_ids = var.subnet-ids

  tags = {
    Name = "${var.environment} ${var.engine} subnet group"
  }
}


resource "aws_security_group" "rds" {
  name        = "${var.environment}-${var.engine}-rds-security-group"
  description = "Security group for rds instance"
  vpc_id      = var.vpc-id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-${var.engine}-rds-security-group",
        )
}

resource "aws_security_group_rule" "rds-allow-nodes" {
  description              = "Allow nodes to communicate with rds"
  from_port                = var.port_number
  to_port                  = var.port_number
  protocol                 = "-1"
  security_group_id        = aws_security_group.rds.id
  source_security_group_id = var.node-security-group-id
  type                     = "ingress"
}

resource "aws_db_parameter_group" "_" {
  name   = "default-${var.engine}"
  family = var.parameter_group_name

}