output "vpc_id" {
  description = "Identificador da VPC do IVIS."
  value       = aws_vpc.principal.id
}

output "sub_redes_publicas" {
  description = "Identificadores das sub-redes públicas, por sufixo de zona."
  value       = { for chave, sub_rede in aws_subnet.publica : chave => sub_rede.id }
}

output "sub_redes_privadas" {
  description = "Identificadores das sub-redes privadas, por sufixo de zona."
  value       = { for chave, sub_rede in aws_subnet.privada : chave => sub_rede.id }
}

output "grupos_de_seguranca" {
  description = "Identificadores dos grupos de segurança (app, monitoramento e rds)."
  value = {
    app           = aws_security_group.app.id
    monitoramento = aws_security_group.monitoramento.id
    rds           = aws_security_group.rds.id
  }
}

output "urls_ecr" {
  description = "URLs dos repositórios ECR, por nome de imagem."
  value       = { for nome, repositorio in aws_ecr_repository.imagem : nome => repositorio.repository_url }
}

output "endereco_banco" {
  description = "Endereço (host) do RDS PostgreSQL. A senha nunca é exposta como saída."
  value       = aws_db_instance.principal.address
}
output "ip_elastico_app" {
  description = "IP elástico da EC2 app (aponta o subdomínio do DuckDNS)."
  value       = aws_eip.app.public_ip
}

output "ip_privado_app" {
  description = "IP privado da EC2 app (alvo do Prometheus na porta 9091)."
  value       = aws_instance.app.private_ip
}

output "ip_publico_monitoramento" {
  description = "IP público da EC2 de monitoramento (muda a cada reinício)."
  value       = aws_instance.monitoramento.public_ip
}

output "ids_instancias" {
  description = "Identificadores das instâncias EC2, para uso com o SSM."
  value = {
    app           = aws_instance.app.id
    monitoramento = aws_instance.monitoramento.id
  }
}