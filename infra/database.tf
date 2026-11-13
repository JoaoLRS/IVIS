# Banco de dados do IVIS: RDS PostgreSQL privado.
# Fica nas sub-redes privadas e só aceita conexão do grupo ivis-rds (EC2 app).
# Configuração pensada para o laboratório: sem cópias, sem snapshot final e sem
# proteção contra exclusão, para permitir o ciclo terraform destroy e apply.

resource "aws_db_subnet_group" "principal" {
  name       = "ivis-banco-sub-redes"
  subnet_ids = [for sub_rede in aws_subnet.privada : sub_rede.id]

  tags = { Name = "ivis-banco-sub-redes" }
}

resource "aws_db_instance" "principal" {
  identifier = "ivis-banco"

  engine         = "postgres"
  engine_version = "17.11"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = "ivis"
  username = "ivis"
  password = var.senha_banco

  db_subnet_group_name   = aws_db_subnet_group.principal.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period    = 0
  skip_final_snapshot        = true
  deletion_protection        = false
  auto_minor_version_upgrade = false
  apply_immediately          = true

  tags = { Name = "ivis-banco" }
}