package com.clinica.estoque.service;

import com.clinica.estoque.config.RabbitConfig;
import com.clinica.estoque.messaging.Eventos;
import com.clinica.estoque.model.Alerta;
import com.clinica.estoque.model.Lote;
import com.clinica.estoque.model.Movimentacao;
import com.clinica.estoque.model.Transferencia;
import com.clinica.estoque.repository.AlertaRepository;
import com.clinica.estoque.repository.LoteRepository;
import com.clinica.estoque.repository.MovimentacaoRepository;
import com.clinica.estoque.repository.TransferenciaRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class EstoqueService {

    private static final long LIMITE_DIAS = 15;

    private final MovimentacaoRepository movimentacaoRepo;
    private final LoteRepository loteRepo;
    private final TransferenciaRepository transferenciaRepo;
    private final AlertaRepository alertaRepo;
    private final RabbitTemplate rabbit;

    public EstoqueService(MovimentacaoRepository movimentacaoRepo, LoteRepository loteRepo,
                          TransferenciaRepository transferenciaRepo, AlertaRepository alertaRepo,
                          RabbitTemplate rabbit) {
        this.movimentacaoRepo = movimentacaoRepo;
        this.loteRepo = loteRepo;
        this.transferenciaRepo = transferenciaRepo;
        this.alertaRepo = alertaRepo;
        this.rabbit = rabbit;
    }

    // ---------- Movimentações (entrada/saída) ----------
    @Transactional
    public Movimentacao registrarMovimentacao(Movimentacao mov) {
        Lote lote = buscarLote(mov.getLote());

        int novoSaldo = mov.getTipo() == Movimentacao.Tipo.ENTRADA
                ? lote.getQuantidade() + mov.getQuantidade()
                : lote.getQuantidade() - mov.getQuantidade();

        if (novoSaldo < 0) {
            throw new IllegalArgumentException("Saldo insuficiente no lote " + lote.getLote()
                    + " (disponível: " + lote.getQuantidade() + ")");
        }
        lote.setQuantidade(novoSaldo);
        loteRepo.save(lote);

        Movimentacao salva = movimentacaoRepo.save(mov);

        rabbit.convertAndSend(RabbitConfig.RK_MOVIMENTACAO, new Eventos.MovimentacaoRegistrada(
                salva.getId(), salva.getTipo().name(), salva.getProduto(), salva.getLote(),
                salva.getQuantidade(), salva.getMotivo(), salva.getData()));

        return salva;
    }

    // ---------- Cadastro de lote (entrada inicial com validade e fornecedor) ----------
    @Transactional
    public Lote cadastrarLote(Lote lote) {
        if (lote.getProduto() == null || lote.getProduto().isBlank()) {
            throw new IllegalArgumentException("O produto é obrigatório.");
        }
        if (lote.getQuantidade() == null || lote.getQuantidade() <= 0) {
            throw new IllegalArgumentException("A quantidade inicial deve ser maior que zero.");
        }
        if (lote.getDataValidade() == null || lote.getDataValidade().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de validade deve ser hoje ou no futuro.");
        }
        lote.setDataEntrada(Instant.now());
        Lote salvo = loteRepo.save(lote);

        // Cadastro de lote gera uma entrada automática no histórico
        rabbit.convertAndSend(RabbitConfig.RK_MOVIMENTACAO, new Eventos.MovimentacaoRegistrada(
                null, "ENTRADA", salvo.getProduto(), salvo.getLote(),
                salvo.getQuantidade(), "Cadastro de lote", Instant.now()));

        return salvo;
    }

    // ---------- Transferências entre empresas ----------
    @Transactional
    public Transferencia transferir(Transferencia t) {
        if (t.getOrigemId().equals(t.getDestinoId())) {
            throw new IllegalArgumentException("Origem e destino não podem ser iguais.");
        }
        Lote lote = buscarLote(t.getLote());

        if (t.getQuantidade() > lote.getQuantidade()) {
            throw new IllegalArgumentException("Quantidade maior que o saldo do lote.");
        }
        lote.setQuantidade(lote.getQuantidade() - t.getQuantidade());
        loteRepo.save(lote);

        Transferencia salva = transferenciaRepo.save(t);

        rabbit.convertAndSend(RabbitConfig.RK_TRANSFERENCIA, new Eventos.TransferenciaSolicitada(
                salva.getId(), salva.getOrigemId(), salva.getDestinoId(),
                salva.getProduto(), salva.getLote(), salva.getQuantidade(), salva.getData()));

        return salva;
    }

    @Transactional
    public Transferencia concluirTransferencia(Long id) {
        Transferencia t = transferenciaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada."));
        t.setStatus(Transferencia.Status.CONCLUIDA);
        return transferenciaRepo.save(t);
    }

    // ---------- Alertas (regra dos 15 dias, persistidos) ----------
    @Transactional
    public List<Alerta> listarAlertas() {
        verificarAlertas();
        return alertaRepo.findAll().stream()
                .sorted(Comparator.comparingLong(Alerta::getDias))
                .toList();
    }

    @Transactional
    public void verificarAlertas() {
        alertaRepo.deleteAll(); // recalcula do zero a cada movimentação
        for (Lote lote : loteRepo.findAll()) {
            if (lote.getQuantidade() <= 0) {
                salvarAlerta(lote, "ZERADO", 0);
                continue;
            }
            long diasVenc = ChronoUnit.DAYS.between(LocalDate.now(), lote.getDataValidade());
            if (diasVenc <= 0) {
                salvarAlerta(lote, "VENCIDO", diasVenc);
            } else if (diasVenc <= LIMITE_DIAS) {
                salvarAlerta(lote, "VENCIMENTO", diasVenc);
            }

            Double consumoDiario = consumoMedioDiario(lote.getProduto());
            if (consumoDiario > 0) {
                long duracao = (long) Math.ceil(lote.getQuantidade() / consumoDiario);
                if (duracao <= LIMITE_DIAS) {
                    salvarAlerta(lote, "DURACAO", duracao);
                }
            }
        }
    }

    private void salvarAlerta(Lote lote, String motivo, long dias) {
        Alerta alerta = new Alerta();
        alerta.setProduto(lote.getProduto());
        alerta.setLote(lote.getLote());
        alerta.setMotivo(motivo);
        alerta.setDias(dias);
        alertaRepo.save(alerta);

        rabbit.convertAndSend(RabbitConfig.RK_ALERTA,
                new Eventos.AlertaEstoque(lote.getProduto(), lote.getLote(), motivo, dias, Instant.now()));
    }

    // ---------- Curva ABC ----------
    public record CurvaABCItem(String produto, long totalSaida, double percentual,
                               double acumulado, String classe) {}

    public List<CurvaABCItem> curvaABC() {
        var saidasPorProduto = new java.util.HashMap<String, Long>();
        for (Movimentacao m : movimentacaoRepo.findAll()) {
            if (m.getTipo() == Movimentacao.Tipo.SAIDA) {
                saidasPorProduto.merge(m.getProduto(), (long) m.getQuantidade(), Long::sum);
            }
        }
        long total = saidasPorProduto.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return List.of();

        var itens = new ArrayList<CurvaABCItem>();
        double acumulado = 0;
        var ordenado = saidasPorProduto.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .toList();
        for (var e : ordenado) {
            double percentual = (e.getValue() * 100.0) / total;
            acumulado += percentual;
            String classe = acumulado <= 80 ? "A" : acumulado <= 95 ? "B" : "C";
            itens.add(new CurvaABCItem(e.getKey(), e.getValue(),
                    Math.round(percentual * 100.0) / 100.0,
                    Math.round(acumulado * 100.0) / 100.0, classe));
        }
        return itens;
    }

    // ---------- Helpers ----------
    private Lote buscarLote(String codigoLote) {
        return loteRepo.findAll().stream()
                .filter(l -> l.getLote().equals(codigoLote))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + codigoLote));
    }

    private Double consumoMedioDiario(String produto) {
        Instant limite = Instant.now().minus(30, ChronoUnit.DAYS);
        int totalSaida = movimentacaoRepo.findAll().stream()
                .filter(m -> m.getProduto().equals(produto)
                        && m.getTipo() == Movimentacao.Tipo.SAIDA
                        && m.getData().isAfter(limite))
                .mapToInt(Movimentacao::getQuantidade)
                .sum();
        return totalSaida / 30.0;
    }
}
