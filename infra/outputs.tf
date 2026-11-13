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