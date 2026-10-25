package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "funcionarios")
@Data
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cep;
    private String email;
    private String cpf;
    private Instant dataNascimento;
    private String cidade;
    private String estado;
}
