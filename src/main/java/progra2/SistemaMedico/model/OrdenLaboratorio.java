package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.*;
import progra2.SistemaMedico.model.enums.EstadoOrdenLaboratorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_laboratorio")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroOrden;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenLaboratorio estado;

    @Column(nullable = false)
    private Boolean externa;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrdenExamen> examenes = new ArrayList<>();

    @Version
    private Long version;

    public void agregarExamen(OrdenExamen examen) {
        examen.setOrden(this);
        examenes.add(examen);
    }
}
