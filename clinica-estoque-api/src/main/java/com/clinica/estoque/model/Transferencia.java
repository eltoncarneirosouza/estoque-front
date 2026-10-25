package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "transferencias")
@Data
public class Transferencia {
    public enum Status { PENDENTE, CONCLUIDA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long origemId;
    private Long destinoId;
    private String produto;
    private String lote;
    private Integer quantidade;
    private Instant data = Instant.now();
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDENTE;
}
