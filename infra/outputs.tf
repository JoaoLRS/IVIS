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