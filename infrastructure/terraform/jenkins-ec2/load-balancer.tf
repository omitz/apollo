resource "aws_lb" "jenkins-master-lb" {
  name               = "dev-apollo-jenkins-master-lb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.jenkins-load-balancer.id]
  subnets            = var.public-subnet-ids

  enable_deletion_protection = false

  tags = {
    Environment = var.environment
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.jenkins-master-lb.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = var.certificate-arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.jenkins-master-target-group.arn
  }
}

resource "aws_lb_listener" "http-redirect" {
  load_balancer_arn = aws_lb.jenkins-master-lb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_target_group" "jenkins-master-target-group" {
  name     = "dev-apollo-jenkins-master-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = var.vpc-id
  target_type = "instance"

  health_check {
    enabled = true
    interval = 30
    path = "/login"
    port = "traffic-port"
    protocol = "HTTP"
    healthy_threshold = 5
    unhealthy_threshold = 10
    timeout = 10
  }
}