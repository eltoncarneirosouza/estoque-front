package com.clinica.estoque.controller;

import com.clinica.estoque.model.Funcionario;
import com.clinica.estoque.repository.FuncionarioRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/administrativo/funcionarios")
public class FuncionarioController {

    private final FuncionarioRepository repository;

    public FuncionarioController(FuncionarioRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Funcionario salvar(@RequestBody Funcionario f) {
        return repository.save(f);
    }

    @GetMapping("/lista")
    public List<Funcionario> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Funcionario buscar(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Funcionário não encontrado."));
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable Long id, @RequestBody Funcionario f) {
        repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Funcionário não encontrado."));
        f.setId(id);
        return repository.save(f);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
