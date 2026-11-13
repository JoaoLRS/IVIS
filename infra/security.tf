# Grupos de segurança do IVIS.
# Sem porta 22: o acesso administrativo é por SSM (Session Manager).
# Sem porta 3000: o Grafana é acessado por túnel SSM (redirecionamento de porta).
# As regras ficam em recursos separados para evitar dependência circular entre grupos.

resource "aws_security_group" "app" {
  name        = "ivis-app"
  description = "EC2 app - HTTP e HTTPS publicos"
  vpc_id      = aws_vpc.principal.id

  tags = { Name = "ivis-app" }
}

resource "aws_security_group" "monitoramento" {
  name        = "ivis-monitoramento"
  description = "EC2 de monitoramento - sem entrada publica"
  vpc_id      = aws_vpc.principal.id

  tags = { Name = "ivis-monitoramento" }
}

resource "aws_security_group" "rds" {
  name        = "ivis-rds"
  description = "RDS PostgreSQL - acesso somente da EC2 app"
  vpc_id      = aws_vpc.principal.id

  tags = { Name = "ivis-rds" }
}

# --- EC2 app ---

resource "aws_vpc_security_group_ingress_rule" "app_http" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP publico - Lets Encrypt e redirecionamento para HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "app_https" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS publico"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_ingress_rule" "app_metricas" {
  security_group_id            = aws_security_group.app.id
  description                  = "Metricas do Actuator (9091) somente do monitoramento"
  referenced_security_group_id = aws_security_group.monitoramento.id
  ip_protocol                  = "tcp"
  from_port                    = 9091
  to_port                      = 9091
}

resource "aws_vpc_security_group_egress_rule" "app_saida" {
  security_group_id = aws_security_group.app.id
  description       = "Saida liberada (imagens Docker, SSM, RDS, Loki)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# --- EC2 de monitoramento ---

resource "aws_vpc_security_group_ingress_rule" "monitoramento_loki" {
  security_group_id            = aws_security_group.monitoramento.id
  description                  = "Loki (3100) somente da EC2 app (Alloy envia logs)"
  referenced_security_group_id = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = 3100
  to_port                      = 3100
}

resource "aws_vpc_security_group_egress_rule" "monitoramento_saida" {
  security_group_id = aws_security_group.monitoramento.id
  description       = "Saida liberada (imagens Docker, SSM, coleta de metricas)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# --- RDS ---
# Sem regra de saída: o banco não inicia conexões.

resource "aws_vpc_security_group_ingress_rule" "rds_postgres" {
  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL (5432) somente da EC2 app"
  referenced_security_group_id = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}