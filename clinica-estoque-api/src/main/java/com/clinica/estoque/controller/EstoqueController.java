package com.clinica.estoque.controller;

import com.clinica.estoque.model.Alerta;
import com.clinica.estoque.model.Lote;
import com.clinica.estoque.model.Movimentacao;
import com.clinica.estoque.model.Transferencia;
import com.clinica.estoque.repository.LoteRepository;
import com.clinica.estoque.repository.MovimentacaoRepository;
import com.clinica.estoque.repository.TransferenciaRepository;
import com.clinica.estoque.service.EstoqueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estoque")
public class EstoqueController {

    private final LoteRepository loteRepo;
    private final MovimentacaoRepository movRepo;
    private final TransferenciaRepository transfRepo;
    private final EstoqueService estoqueService;

    public EstoqueController(LoteRepository loteRepo, MovimentacaoRepository movRepo,
                             TransferenciaRepository transfRepo, EstoqueService estoqueService) {
        this.loteRepo = loteRepo;
        this.movRepo = movRepo;
        this.transfRepo = transfRepo;
        this.estoqueService = estoqueService;
    }

    // Lotes
    @GetMapping("/lotes")
    public List<Lote> lotes() {
        return loteRepo.findAll();
    }

    @PostMapping("/lotes")
    public Lote cadastrarLote(@RequestBody Lote lote) {
        return estoqueService.cadastrarLote(lote);
    }

    // Movimentações
    @PostMapping("/movimentacoes")
    public Movimentacao registrarMovimentacao(@RequestBody Movimentacao mov) {
        return estoqueService.registrarMovimentacao(mov);
    }

    @GetMapping("/movimentacoes")
    public List<Movimentacao> movimentacoes() {
        return movRepo.findAll();
    }

    // Transferências
    @PostMapping("/transferencias")
    public Transferencia transferir(@RequestBody Transferencia t) {
        return estoqueService.transferir(t);
    }

    @GetMapping("/transferencias")
    public List<Transferencia> transferencias() {
        return transfRepo.findAll();
    }

    @PatchMapping("/transferencias/{id}/concluir")
    public Transferencia concluirTransferencia(@PathVariable Long id) {
        return estoqueService.concluirTransferencia(id);
    }

    // Alertas (regra dos 15 dias, persistidos)
    @GetMapping("/alertas")
    public List<Alerta> alertas() {
        return estoqueService.listarAlertas();
    }

    // Curva ABC
    @GetMapping("/curva-abc")
    public List<EstoqueService.CurvaABCItem> curvaABC() {
        return estoqueService.curvaABC();
    }
}
