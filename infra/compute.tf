# Computação do IVIS: duas EC2 (app e monitoramento), IP elástico e AMI do Amazon Linux 2023.
# Acesso administrativo por SSM (sem porta 22). Sem NAT: as EC2 saem pela internet via gateway.

locals {
  # Versão do plugin do Docker Compose (conferida em github.com/docker/compose/releases).
  versao_compose = "v5.7.1"

  user_data = <<-EOT
    #!/bin/bash
    set -euo pipefail
    exec > >(tee -a /var/log/ivis-user-data.log) 2>&1

    echo "[ivis] inicio: $(date -u +%FT%TZ)"

    dnf install -y docker
    systemctl enable --now docker
    usermod -aG docker ec2-user

    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -fsSL "https://github.com/docker/compose/releases/download/${local.versao_compose}/docker-compose-linux-x86_64" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

    docker --version
    docker compose version
    echo "[ivis] fim: $(date -u +%FT%TZ)"
  EOT
}

# AMI do Amazon Linux 2023 pelo parâmetro público do SSM (sem fixar o ID).
data "aws_ssm_parameter" "ami_al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_instance" "app" {
  ami                    = data.aws_ssm_parameter.ami_al2023.value
  instance_type          = "t3.medium"
  subnet_id              = aws_subnet.publica["a"].id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = "LabInstanceProfile"

  user_data                   = local.user_data
  user_data_replace_on_change = false

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 20
    encrypted   = true
  }

  tags = { Name = "ivis-app" }

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

resource "aws_eip" "app" {
  domain = "vpc"

  tags = { Name = "ivis-app-eip" }

  depends_on = [aws_internet_gateway.principal]
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}

resource "aws_instance" "monitoramento" {
  ami                    = data.aws_ssm_parameter.ami_al2023.value
  instance_type          = "t3.small"
  subnet_id              = aws_subnet.publica["b"].id
  vpc_security_group_ids = [aws_security_group.monitoramento.id]
  iam_instance_profile   = "LabInstanceProfile"

  user_data                   = local.user_data
  user_data_replace_on_change = false

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 20
    encrypted   = true
  }

  tags = { Name = "ivis-monitoramento" }

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}