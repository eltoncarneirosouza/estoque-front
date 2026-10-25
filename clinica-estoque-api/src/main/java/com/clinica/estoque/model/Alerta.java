package com.clinica.estoque.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "alertas")
@Data
public class Alerta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String produto;
    private String lote;
    private String motivo;   // ZERADO, VENCIDO, VENCIMENTO, DURACAO
    private long dias;
    private Instant geradoEm = Instant.now();
    private boolean lido = false;
}
