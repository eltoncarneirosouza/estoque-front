package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "empresas")
@Data
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
}
