resource "kubernetes_config_map" "aws-auth" {
    metadata {
        namespace = "kube-system"
        name = "aws-auth"
    }

    lifecycle {
        ignore_changes = [
            data,
        ]
    }

    data = {
     mapRoles = <<ROLES
- rolearn: ${var.aws-iam-role-node-arn}
  username: system:node:{{EC2PrivateDNSName}}
  groups:
    - system:bootstrappers
    - system:nodes
- rolearn: arn:aws:iam::${var.aws-account-id}:role/${var.jenkins-iam-role-name}
    username: jenkins
    groups:
    - system:masters
ROLES
    mapUsers = <<USERS
%{ for user in var.users }
- userarn: arn:aws:iam::${var.aws-account-id}:user/${user}
  username: ${user}
  groups:
    - system:masters
%{ endfor }
USERS
}
}
