package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "receta_detalles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(nullable = false, length = 100)
    private String frecuencia;

    @Column(nullable = false, length = 100)
    private String duracion;

    @Column(length = 500)
    private String indicaciones;
}
