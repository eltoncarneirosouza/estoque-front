package com.clinica.estoque.config;

import com.clinica.estoque.model.*;
import com.clinica.estoque.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final LoteRepository loteRepo;
    private final MovimentacaoRepository movRepo;
    private final EmpresaRepository empresaRepo;
    private final ProdutoRepository produtoRepo;
    private final FornecedorRepository fornecedorRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarioRepo, FuncionarioRepository funcionarioRepo,
                           LoteRepository loteRepo, MovimentacaoRepository movRepo,
                           EmpresaRepository empresaRepo, ProdutoRepository produtoRepo,
                           FornecedorRepository fornecedorRepo, PasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.funcionarioRepo = funcionarioRepo;
        this.loteRepo = loteRepo;
        this.movRepo = movRepo;
        this.empresaRepo = empresaRepo;
        this.produtoRepo = produtoRepo;
        this.fornecedorRepo = fornecedorRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // Usuário de teste (mesmo do curl): eltoncarneiro55@gmail.com / 1234
        if (usuarioRepo.findByEmail("eltoncarneiro55@gmail.com").isEmpty()) {
            Usuario u = new Usuario();
            u.setNome("Elton Carneiro");
            u.setEmail("eltoncarneiro55@gmail.com");
            u.setSenha(encoder.encode("1234"));
            usuarioRepo.save(u);
        }

        if (empresaRepo.count() == 0) {
            empresaRepo.save(emp("Clínica Central (Matriz)"));
            empresaRepo.save(emp("Clínica Norte (Filial)"));
            empresaRepo.save(emp("Clínica Sul (Filial)"));
        }

        if (loteRepo.count() == 0) {
            loteRepo.save(lote("Seringa 5ml", "SRG-2026-A", "MedInject", 500, 180));
            loteRepo.save(lote("Luva de Látex M", "LTX-118", "SafeGlove", 45, 40));
            loteRepo.save(lote("Álcool 70% 1L", "ALC-77", "QuímicaClean", 0, 230));
            loteRepo.save(lote("Gaze Estéril", "GZ-332", "MedTex", 320, 470));
            loteRepo.save(lote("Paracetamol 750mg", "PRC-554", "FarmaBrasil", 18, 300));
            loteRepo.save(lote("Soro Fisiológico 500ml", "SRF-091", "MedInject", 200, 11));
        }

        if (movRepo.count() == 0) {
            movRepo.save(mov(Movimentacao.Tipo.SAIDA, "Seringa 5ml", "SRG-2026-A", 60, "Atendimento ambulatório", 5));
            movRepo.save(mov(Movimentacao.Tipo.SAIDA, "Paracetamol 750mg", "PRC-554", 30, "Receituário", 3));
            movRepo.save(mov(Movimentacao.Tipo.SAIDA, "Paracetamol 750mg", "PRC-554", 40, "Urgência", 15));
            movRepo.save(mov(Movimentacao.Tipo.SAIDA, "Soro Fisiológico 500ml", "SRF-091", 12, "Hidratação", 4));
        }

        if (produtoRepo.count() == 0) {
            produtoRepo.save(prod("Seringa 5ml", "Insumos", "Seringa descartável 5ml"));
            produtoRepo.save(prod("Luva de Látex M", "EPIs", "Luva procedimento tamanho M"));
            produtoRepo.save(prod("Álcool 70% 1L", "Medicamentos", "Álcool etílico 70% 1 litro"));
            produtoRepo.save(prod("Gaze Estéril", "Insumos", "Gaze estéril 7,5cm x 7,5cm"));
            produtoRepo.save(prod("Paracetamol 750mg", "Medicamentos", "Comprimido 750mg"));
            produtoRepo.save(prod("Soro Fisiológico 500ml", "Medicamentos", "NaCl 0,9% 500ml"));
        }

        if (fornecedorRepo.count() == 0) {
            fornecedorRepo.save(forn("MedInject Ltda", "12.345.678/0001-90", "vendas@medinject.com", "(41) 3000-1000"));
            fornecedorRepo.save(forn("SafeGlove Brasil", "98.765.432/0001-10", "contato@safeglove.com", "(41) 3000-2000"));
            fornecedorRepo.save(forn("FarmaBrasil Distribuidora", "11.222.333/0001-44", "pedidos@farmabrasil.com", "(41) 3000-3000"));
        }

        if (funcionarioRepo.count() == 0) {
            Funcionario f = new Funcionario();
            f.setNome("Tatiane Pereira");
            f.setCep("82630490");
            f.setEmail("tatiane@gmail.com");
            f.setCpf("03248549947");
            f.setDataNascimento(Instant.parse("1981-09-20T18:59:44.223Z"));
            funcionarioRepo.save(f);
        }
    }

    private Produto prod(String nome, String categoria, String descricao) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setCategoria(categoria);
        p.setDescricao(descricao);
        return p;
    }

    private Fornecedor forn(String nome, String cnpj, String email, String telefone) {
        Fornecedor f = new Fornecedor();
        f.setNome(nome);
        f.setCnpj(cnpj);
        f.setEmail(email);
        f.setTelefone(telefone);
        return f;
    }

    private Empresa emp(String nome) {
        Empresa e = new Empresa();
        e.setNome(nome);
        return e;
    }

    private Lote lote(String produto, String lote, String fabricante, int qtd, long diasValidade) {
        Lote l = new Lote();
        l.setProduto(produto);
        l.setLote(lote);
        l.setFabricante(fabricante);
        l.setQuantidade(qtd);
        l.setDataValidade(LocalDate.now().plusDays(diasValidade));
        l.setDataEntrada(Instant.now().minus(30, ChronoUnit.DAYS));
        return l;
    }

    private Movimentacao mov(Movimentacao.Tipo tipo, String produto, String lote,
                             int qtd, String motivo, long diasAtras) {
        Movimentacao m = new Movimentacao();
        m.setTipo(tipo);
        m.setProduto(produto);
        m.setLote(lote);
        m.setQuantidade(qtd);
        m.setMotivo(motivo);
        m.setData(Instant.now().minus(diasAtras, ChronoUnit.DAYS));
        return m;
    }
}
