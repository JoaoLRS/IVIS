variable "regiao" {
  description = "Região da AWS usada pelo projeto."
  type        = string
  default     = "us-east-1"
}

variable "senha_banco" {
  description = "Senha do usuário administrador do RDS. Informe por TF_VAR_senha_banco; nunca versionar."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.senha_banco) >= 12 && can(regex("^[^/@\" ]+$", var.senha_banco))
    error_message = "A senha deve ter pelo menos 12 caracteres e não pode conter barra, arroba, aspas ou espaço."
  }
}