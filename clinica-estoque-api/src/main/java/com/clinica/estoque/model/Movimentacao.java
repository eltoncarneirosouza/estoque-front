package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "movimentacoes")
@Data
public class Movimentacao {
    public enum Tipo { ENTRADA, SAIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    private String produto;
    private String lote;
    private Integer quantidade;
    private String motivo;
    private Instant data = Instant.now();
}
