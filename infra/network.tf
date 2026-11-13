# Rede do IVIS: VPC, sub-redes públicas (EC2) e privadas (RDS), gateway de internet.
# Sem NAT Gateway: as EC2 ficam em sub-rede pública e o RDS privado não precisa de saída.

locals {
  cidr_vpc = "10.0.0.0/16"

  sub_redes_publicas = {
    a = { cidr = "10.0.1.0/24", zona = "${var.regiao}a" }
    b = { cidr = "10.0.2.0/24", zona = "${var.regiao}b" }
  }

  sub_redes_privadas = {
    a = { cidr = "10.0.11.0/24", zona = "${var.regiao}a" }
    b = { cidr = "10.0.12.0/24", zona = "${var.regiao}b" }
  }
}

resource "aws_vpc" "principal" {
  cidr_block           = local.cidr_vpc
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "ivis-vpc" }
}

resource "aws_internet_gateway" "principal" {
  vpc_id = aws_vpc.principal.id

  tags = { Name = "ivis-igw" }
}

resource "aws_subnet" "publica" {
  for_each = local.sub_redes_publicas

  vpc_id                  = aws_vpc.principal.id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.zona
  map_public_ip_on_launch = true

  tags = { Name = "ivis-publica-${each.key}" }
}

resource "aws_subnet" "privada" {
  for_each = local.sub_redes_privadas

  vpc_id                  = aws_vpc.principal.id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.zona
  map_public_ip_on_launch = false

  tags = { Name = "ivis-privada-${each.key}" }
}

resource "aws_route_table" "publica" {
  vpc_id = aws_vpc.principal.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.principal.id
  }

  tags = { Name = "ivis-rotas-publicas" }
}

resource "aws_route_table" "privada" {
  vpc_id = aws_vpc.principal.id

  tags = { Name = "ivis-rotas-privadas" }
}

resource "aws_route_table_association" "publica" {
  for_each = aws_subnet.publica

  subnet_id      = each.value.id
  route_table_id = aws_route_table.publica.id
}

resource "aws_route_table_association" "privada" {
  for_each = aws_subnet.privada

  subnet_id      = each.value.id
  route_table_id = aws_route_table.privada.id
}