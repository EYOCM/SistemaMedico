package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.*;
import progra2.SistemaMedico.model.enums.EstadoReceta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroReceta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReceta estado;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RecetaDetalle> detalles = new ArrayList<>();

    @Version
    private Long version;

    public void agregarDetalle(RecetaDetalle detalle) {
        detalle.setReceta(this);
        detalles.add(detalle);
    }
}
