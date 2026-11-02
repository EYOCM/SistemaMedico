package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signos_vitales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignosVitales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(nullable = false)
    private Integer sistolica;

    @Column(nullable = false)
    private Integer diastolica;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal temperatura;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal talla;

    @Column(nullable = false)
    private Integer frecuenciaCardiaca;

    @Column(length = 500)
    private String alertas;

    @Column(length = 50)
    private String registradoPor;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;
}