package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "lotes")
@Data
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String produto;
    private String lote;
    private String fabricante;
    private Integer quantidade;
    private LocalDate dataValidade;
    private Instant dataEntrada;
}
