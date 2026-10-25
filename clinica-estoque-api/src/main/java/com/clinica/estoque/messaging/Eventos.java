package com.clinica.estoque.messaging;

import java.time.Instant;

// Eventos trocados via RabbitMQ (JSON)
public class Eventos {

    public record MovimentacaoRegistrada(
            Long id, String tipo, String produto, String lote,
            Integer quantidade, String motivo, Instant data) {}

    public record AlertaEstoque(
            String produto, String lote, String motivo,
            long dias, Instant geradoEm) {}

    public record TransferenciaSolicitada(
            Long id, Long origemId, Long destinoId,
            String produto, String lote, Integer quantidade, Instant data) {}
}
