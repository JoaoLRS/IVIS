# Repositórios de imagens do IVIS (ECR).
# Tags imutáveis: o pipeline marca cada imagem com o SHA do commit.
# force_delete permite o ciclo terraform destroy mesmo com imagens no repositório.

locals {
  repositorios_ecr = toset(["ivis-backend", "ivis-api-ia", "ivis-frontend"])
}

resource "aws_ecr_repository" "imagem" {
  for_each = local.repositorios_ecr

  name                 = each.key
  image_tag_mutability = "IMMUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = { Name = each.key }
}