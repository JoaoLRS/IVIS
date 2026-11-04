export const requisitosSenha = [
  { id: "tamanho", texto: "Pelo menos 8 caracteres", verificar: (senha: string) => senha.length >= 8 },
  { id: "maiuscula", texto: "Uma letra maiúscula", verificar: (senha: string) => /\p{Lu}/u.test(senha) },
  { id: "minuscula", texto: "Uma letra minúscula", verificar: (senha: string) => /\p{Ll}/u.test(senha) },
  { id: "numero", texto: "Um número", verificar: (senha: string) => /[0-9]/.test(senha) },
  { id: "especial", texto: "Um caractere especial", verificar: (senha: string) => /[^\p{L}\p{N}\s]/u.test(senha) },
] as const;

export function senhaValida(senha: string): boolean {
  return senha.length <= 128 && requisitosSenha.every((requisito) => requisito.verificar(senha));
}
