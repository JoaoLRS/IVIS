package br.ivis.identity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ivis.identity.model.SessaoAutenticacaoModel;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessaoAutenticacaoModelTest {
    @Test
    void exigeNovoLoginAoAtingirVinteMinutosSemAtividade() {
        Instant inicio = Instant.parse("2026-01-01T12:00:00Z");
        SessaoAutenticacaoModel sessao = new SessaoAutenticacaoModel(
                UUID.randomUUID(), UUID.randomUUID(), "hash", inicio.plusSeconds(7 * 24 * 3600), inicio);

        assertTrue(sessao.estaAtivaAposInatividade(inicio.plusSeconds(19 * 60 + 59), 20));
        assertFalse(sessao.estaAtivaAposInatividade(inicio.plusSeconds(20 * 60), 20));

        sessao.registrarAtividade(inicio.plusSeconds(10 * 60));
        assertTrue(sessao.estaAtivaAposInatividade(inicio.plusSeconds(25 * 60), 20));
        assertFalse(sessao.estaAtivaAposInatividade(inicio.plusSeconds(30 * 60), 20));
    }
}
