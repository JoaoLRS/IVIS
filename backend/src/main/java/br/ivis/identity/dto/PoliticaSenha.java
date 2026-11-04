package br.ivis.identity.dto;

public final class PoliticaSenha {
    public static final String EXPRESSAO = "(?s)^(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).+$";
    public static final String MENSAGEM =
            "A senha deve ter uma letra maiúscula, uma minúscula, um número e um caractere especial.";

    private PoliticaSenha() { }
}
