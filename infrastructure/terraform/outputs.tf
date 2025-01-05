output "kubeconfig" {
  value = module.cluster.kubeconfig
}

output "config_map_aws_auth" {
  value = module.cluster.config_map_aws_auth
}

output "vpc-id" {
  value = module.cluster.vpc-id
}

output "cluster-name" {
  value = module.cluster.cluster-name
}

output "postgres_rds_password" {
  value = module.postgres_rds.password
}

output "mysql_rds_password" {
  value = module.mysql_rds.password
}