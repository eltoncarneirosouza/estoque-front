package com.clinica.estoque.controller;

import com.clinica.estoque.model.Fornecedor;
import com.clinica.estoque.repository.FornecedorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorRepository repository;

    public FornecedorController(FornecedorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Fornecedor> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Fornecedor salvar(@RequestBody Fornecedor f) {
        if (f.getNome() == null || f.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do fornecedor é obrigatório.");
        }
        return repository.save(f);
    }
}
