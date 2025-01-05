resource "aws_vpc" "vpc" {
    cidr_block = "10.0.0.0/16"
    #TODO ^ evaluate how large of a cidr block is desired

    tags = map(
        "Name", "${var.environment}-apollo-vpc",
        "kubernetes.io/cluster/${var.cluster-name}", "shared",
      )
}

resource "aws_subnet" "public_subnets" {
  count = 2

  availability_zone = data.aws_availability_zones.available.names[count.index]
  cidr_block        = "10.0.${count.index * 64}.0/18"
  vpc_id            = aws_vpc.vpc.id

  tags = map(
      "Name", "${var.environment}-apollo-public-subnet-${count.index}",
      "kubernetes.io/cluster/${var.cluster-name}", "shared",
      "kubernetes.io/role/elb", 1
    )
}

resource "aws_subnet" "private_subnets" {
  count = 2

  availability_zone = data.aws_availability_zones.available.names[count.index]
  cidr_block        = "10.0.${(count.index + 2) * 64}.0/18"
  vpc_id            = aws_vpc.vpc.id

  tags = map(
      "Name", "${var.environment}-apollo-private-subnet-${count.index}",
      "kubernetes.io/role/elb", 1
    )
}

resource "aws_internet_gateway" "internet_gateway" {
  vpc_id = aws_vpc.vpc.id

  tags = {
    Name = "${var.environment}-apollo-internet-gateway"
  }
}

resource "aws_eip" "nat" {
  count = 2
}

resource "aws_nat_gateway" "nat_gateway" {
  count = 2
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public_subnets[count.index].id
  tags = {
    Name = "${var.environment}-apollo-nat-gateway"
  }
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.internet_gateway.id
  }
}

resource "aws_route_table" "private_route_table" {
  vpc_id = aws_vpc.vpc.id
  count = 2

  route {
    cidr_block = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gateway[count.index].id
  }
}

resource "aws_route_table_association" "public_route_table_association" {
  count = 2

  subnet_id      = aws_subnet.public_subnets.*.id[count.index]
  route_table_id = aws_route_table.public_route_table.id
  
}

resource "aws_route_table_association" "private_route_table_association" {
  count = 2

  subnet_id         = aws_subnet.private_subnets.*.id[count.index]
  route_table_id    = aws_route_table.private_route_table[count.index].id

}

resource "aws_security_group" "cluster" {
  name        = "${var.environment}-apollo-cluster-security-group"
  description = "Cluster communication with worker nodes"
  vpc_id      = aws_vpc.vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.environment}-apollo-cluster-security-group"
  }
}

resource "aws_security_group" "node" {
  name        = "${var.environment}-apollo-node-security-group"
  description = "Security group for all nodes in the cluster"
  vpc_id      = aws_vpc.vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-apollo-node-security-group",
          "kubernetes.io/cluster/${var.cluster-name}", "owned",
        )
}

resource "aws_security_group" "public" {
  name        = "${var.environment}-apollo-lb-security-group"
  description = "Security group for Load Balancers created by cluster"
  vpc_id      = aws_vpc.vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = map(
          "Name", "${var.environment}-apollo-node-security-group",
          "kubernetes.io/cluster/${var.cluster-name}", "owned",
        )
}

resource "aws_security_group_rule" "node-ingress-self" {
  description              = "Allow node to communicate with each other"
  from_port                = 0
  protocol                 = "-1"
  security_group_id        = aws_security_group.node.id
  source_security_group_id = aws_security_group.node.id
  to_port                  = 65535
  type                     = "ingress"
}

resource "aws_security_group_rule" "allow_traffic_from_public_to_private" {
  description              = "Allow worker traffic from public subnet to private"
  from_port                = 0
  protocol                 = "tcp"
  security_group_id        = aws_security_group.node.id
  source_security_group_id = aws_security_group.public.id
  to_port                  = 65535
  type                     = "ingress"
}

resource "aws_security_group_rule" "node-ingress-cluster" {
  description              = "Allow worker Kubelets and pods to receive communication from the cluster control plane"
  from_port                = 1025
  protocol                 = "tcp"
  security_group_id        = aws_security_group.node.id
  source_security_group_id = aws_security_group.cluster.id
  to_port                  = 65535
  type                     = "ingress"
}

resource "aws_security_group_rule" "cluster-ingress-node-https" {
  description              = "Allow nodes to communicate with the cluster API Server"
  from_port                = 443
  protocol                 = "tcp"
  security_group_id        = aws_security_group.cluster.id
  source_security_group_id = aws_security_group.node.id
  to_port                  = 443
  type                     = "ingress"
}