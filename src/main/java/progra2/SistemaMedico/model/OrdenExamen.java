package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_examenes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrdenLaboratorio orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id", nullable = false)
    private Examen examen;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(length = 200)
    private String valorResultado;

    @Column(length = 50)
    private String unidadResultado;

    @Column
    private LocalDateTime fechaResultado;

    @Column(nullable = false)
    @Builder.Default
    private Boolean fueraDeRango = false;

    @Column(length = 1000)
    private String notasResultado;

    @Column(nullable = false)
    @Builder.Default
    private Boolean publicado = false;

    @Column
    private LocalDateTime fechaPublicacion;

    @Version
    private Long version;
}
