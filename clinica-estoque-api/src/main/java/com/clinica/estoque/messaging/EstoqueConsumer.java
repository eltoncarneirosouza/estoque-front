package com.clinica.estoque.messaging;

import com.clinica.estoque.config.RabbitConfig;
import com.clinica.estoque.service.EstoqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EstoqueConsumer {

    private static final Logger log = LoggerFactory.getLogger(EstoqueConsumer.class);
    private final EstoqueService estoqueService;

    public EstoqueConsumer(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    // Ao registrar uma movimentação, recalcula os alertas do estoque
    @RabbitListener(queues = RabbitConfig.FILA_MOVIMENTACAO)
    public void aoRegistrarMovimentacao(Eventos.MovimentacaoRegistrada evento) {
        log.info("[RabbitMQ] Movimentação {}: {} x{} (lote {})",
                evento.tipo(), evento.produto(), evento.quantidade(), evento.lote());
        estoqueService.verificarAlertas();
    }

    // Consome os alertas gerados (aqui pode plugar e-mail, webhook, notificação push...)
    @RabbitListener(queues = RabbitConfig.FILA_ALERTA)
    public void aoGerarAlerta(Eventos.AlertaEstoque alerta) {
        log.warn("[ALERTA ESTOQUE] {} (lote {}) - motivo={} dias={}",
                alerta.produto(), alerta.lote(), alerta.motivo(), alerta.dias());
        // TODO: enviar e-mail/notificação para o responsável pelo estoque
    }

    @RabbitListener(queues = "estoque.transferencia.queue")
    public void aoSolicitarTransferencia(Eventos.TransferenciaSolicitada t) {
        log.info("[RabbitMQ] Transferência #{}: {} x{} do setor {} para {}",
                t.id(), t.produto(), t.quantidade(), t.origemId(), t.destinoId());
    }
}
