package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "fornecedores")
@Data
public class Fornecedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    private String cnpj;
    private String email;
    private String telefone;
}
