//Autoscaling is now managed by kubernetes cluster autoscaler,

/*resource "aws_cloudwatch_metric_alarm" "mem_user_gt_42" {
  alarm_name                = "ASG mem_usage > 42 percent"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = "1"
  metric_name               = "mem_used_percent"
  namespace                 = "CWAgent"
  period                    = "30"
  statistic                 = "Average"
  threshold                 = "42"
  insufficient_data_actions = []

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group._.name
  }

  alarm_actions     = [aws_autoscaling_policy.scale_up.arn]

}

resource "aws_cloudwatch_metric_alarm" "mem_user_gt_20" {
  alarm_name                = "ASG mem_usage > 20 percent"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = "5"
  metric_name               = "mem_used_percent"
  namespace                 = "CWAgent"
  period                    = "60"
  statistic                 = "Average"
  threshold                 = "20"
  insufficient_data_actions = []

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group._.name
  }

  ok_actions     = [aws_autoscaling_policy.scale_down.arn]

}

resource "aws_autoscaling_policy" "scale_up" {
  name                   = "scale_up"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 30
  autoscaling_group_name = aws_autoscaling_group._.name
}

resource "aws_autoscaling_policy" "scale_down" {
  name                   = "scale_down"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group._.name
}*/