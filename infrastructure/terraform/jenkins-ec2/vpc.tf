resource "aws_security_group" "jenkins-master" {
  name        = "${var.environment}-apollo-jenkins-master-security-group"
  description = "Security group for jenkins master"
  vpc_id      = var.vpc-id

  egress {
    from_port   = 0
    to_port = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-jenkins-master-security-group",
        )
}

resource "aws_security_group" "jenkins-agent" {
  name        = "${var.environment}-apollo-jenkins-agent-security-group"
  description = "Security group for jenkins agents"
  vpc_id      = var.vpc-id

  egress {
    from_port   = 0
    to_port = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-jenkins-agent-security-group",
        )
}

resource "aws_security_group" "jenkins-load-balancer" {
  
  lifecycle {
    ignore_changes = [
      ingress,
    ]
  }

  name        = "${var.environment}-apollo-jenkins-load-balancer-security-group"
  description = "Security group for jenkins load balancer"
  vpc_id      = var.vpc-id

  ingress {
    # TLS (change to whatever ports you need)
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["50.225.83.2/32", "192.30.252.0/22", "185.199.108.0/22", "140.82.112.0/20"]

  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-jenkins-load-balancer-security-group",
        )
}


resource "aws_security_group" "jenkins-master-efs" {

  lifecycle {
    ignore_changes = [
      ingress,
    ]
  }

  name        = "${var.environment}-apollo-jenkins-efs-security-group"
  description = "Security group for jenkins efs"
  vpc_id      = var.vpc-id

  egress {
    from_port   = 0
    to_port = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-jenkins-master-efs-security-group",
        )
}

resource "aws_security_group_rule" "jenkins-allow-master-to-agents" {
  description              = "Allow jenkins master to communicate with jenkins agents"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  security_group_id        = aws_security_group.jenkins-master.id
  source_security_group_id = aws_security_group.jenkins-agent.id
  type                     = "ingress"
}

resource "aws_security_group_rule" "jenkins-allow-agents-to-master" {
  description              = "Allow jenkins agents to communicate with jenkins master"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  security_group_id        = aws_security_group.jenkins-agent.id
  source_security_group_id = aws_security_group.jenkins-master.id
  type                     = "ingress"
}

resource "aws_security_group_rule" "jenkins-allow-efs-to-master" {
  description              = "Allow jenkins-efs to communicate with jenkins master node"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  security_group_id        = aws_security_group.jenkins-master-efs.id
  source_security_group_id = aws_security_group.jenkins-master.id
  type                     = "ingress"
}

resource "aws_security_group_rule" "jenkins-allow-master-to-efs" {
  description              = "Allow jenkins master node to communicate with jenkins efs"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  security_group_id        = aws_security_group.jenkins-master.id
  source_security_group_id = aws_security_group.jenkins-master-efs.id
  type                     = "ingress"
}

resource "aws_security_group_rule" "jenkins-master-allow-load-balancer" {
  description              = "Allow lb to communicate with jenkins"
  from_port                = 0
  to_port                  = 8080
  protocol                 = "-1"
  security_group_id        = aws_security_group.jenkins-master.id
  source_security_group_id = aws_security_group.jenkins-load-balancer.id
  type                     = "ingress"
}