package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import progra2.SistemaMedico.model.enums.PrioridadTarea;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas_agenda", indexes = @Index(name = "idx_tarea_medico_fecha", columnList = "medico_id, fecha_limite"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TareaAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario medico;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadTarea prioridad;

    @Column(nullable = false)
    private LocalDate fechaLimite;

    @Column(nullable = false)
    private Boolean completada;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;
}
