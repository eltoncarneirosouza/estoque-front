package com.clinica.estoque.controller;

import com.clinica.estoque.model.Empresa;
import com.clinica.estoque.repository.EmpresaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaRepository repository;

    public EmpresaController(EmpresaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Empresa> listar() {
        return repository.findAll();
    }
}
