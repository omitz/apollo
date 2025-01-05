data "aws_ami" "eks-worker" {
  filter {
    name   = "name"
    values = ["amazon-eks-node-${aws_eks_cluster.cluster.version}-v*"]
  }

  most_recent = true
  owners      = ["602401143452"] # Amazon EKS AMI Account ID
}


locals {
  node-userdata = <<USERDATA
#!/bin/bash
set -o xtrace
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
rpm -U ./amazon-cloudwatch-agent.rpm
aws s3 cp s3://apollo-source-data/local/aws-cloudwatch-agent-config.json /opt/aws/amazon-cloudwatch-agent/etc/common-config.json
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/common-config.json -s
/etc/eks/bootstrap.sh --apiserver-endpoint '${aws_eks_cluster.cluster.endpoint}' --b64-cluster-ca '${aws_eks_cluster.cluster.certificate_authority.0.data}' '${var.cluster-name}'
USERDATA
}

resource "aws_launch_configuration" "node" {

  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.node.name
  image_id                    = data.aws_ami.eks-worker.id
  instance_type               = "r5.xlarge"
  name_prefix                 = "${var.environment}-apollo-node-ec2"
  security_groups             = [aws_security_group.node.id]
  user_data_base64            = base64encode(local.node-userdata)
  key_name                    = aws_key_pair._.key_name

  lifecycle {
    create_before_destroy = true
    ignore_changes = [
      image_id,
    ]
  }

  root_block_device {
    volume_size           = "64"
    volume_type           = "gp2"
    delete_on_termination = true
    encrypted             = true
  }

}

resource "aws_autoscaling_group" "_" {
  lifecycle {
    ignore_changes = [
      desired_capacity,
    ]
  }

  desired_capacity     = var.desired_nodes
  launch_configuration = aws_launch_configuration.node.id
  max_size             = var.max_nodes
  min_size             = var.min_nodes
  name                 = "${var.environment}-apollo-node-autoscalinggroup"
  vpc_zone_identifier  = aws_subnet.private_subnets.*.id

  tag {
    key                 = "Name"
    value               = "${var.environment}-apollo-node-autoscalinggroup"
    propagate_at_launch = true
  }

  tag {
    key                 = "kubernetes.io/cluster/${var.cluster-name}"
    value               = "owned"
    propagate_at_launch = true
  }

  tag {
    key                 = "kubernetes.io/cluster-autoscaler/${var.cluster-name}"
    value               = "owned"
    propagate_at_launch = true
  }

  tag {
    key                 = "kubernetes.io/cluster-autoscaler/enabled"
    value               = "true"
    propagate_at_launch = true
  }
}

resource "aws_key_pair" "_" {
  key_name = "${var.cluster-name}-key"
  public_key = tls_private_key._.public_key_openssh
}

resource "tls_private_key" "_" {
  algorithm = "RSA"
  rsa_bits  = 4096
}