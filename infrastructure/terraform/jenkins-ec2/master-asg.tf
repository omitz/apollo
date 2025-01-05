/*data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}
^^^ most recent ubuntu ami
*/

data "template_file" "master-user-data" {
  template = file("${path.module}/master-user-data.tpl")

  vars = {
    efs-id = aws_efs_file_system.jenkins-efs.id
    region = var.aws-region
  }
}

resource "aws_autoscaling_group" "jenkins-master-asg" {
  name_prefix               = "dev-apollo-jenkins-master-asg"
  target_group_arns         = [aws_lb_target_group.jenkins-master-target-group.arn]

  termination_policies = [
     "OldestInstance" 
  ]

  default_cooldown          = 30
  health_check_grace_period = 30
  max_size                  = 1
  min_size                  = 0
  desired_capacity          = 1

  launch_configuration      = aws_launch_configuration.jenkins-master-launch-config.name

  lifecycle {
    create_before_destroy = true
  }

  vpc_zone_identifier = var.private-subnet-ids

  tags = [
    {
      key                 = "Name"
      value               = "dev-apollo-jenkins-master-asg",

      propagate_at_launch = true
    }
  ]
}

resource "aws_launch_configuration" "jenkins-master-launch-config" {
  name_prefix                 = "jenkins-master-asg-launch-config"
  image_id                    = "ami-07be5743f8e5bf764" #ubuntu with requirements pre-installed
  #image_id                    = data.aws_ami.ubuntu.id #plain ubuntu
  instance_type               = "t3a.medium"

  enable_monitoring           = true
  associate_public_ip_address = true
  lifecycle {
    create_before_destroy = true
  }

  user_data = base64encode(data.template_file.master-user-data.rendered)


  security_groups = [
    aws_security_group.jenkins-master.id
  ]

  key_name             = "jenkins-key"

  iam_instance_profile = aws_iam_instance_profile.master-instance-profile.name
}

resource "aws_key_pair" "_" {
  key_name = "jenkins-key"
  public_key = tls_private_key._.public_key_openssh
}

resource "tls_private_key" "_" {
  algorithm = "RSA"
  rsa_bits  = 4096
}
