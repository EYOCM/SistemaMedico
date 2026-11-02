package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import progra2.SistemaMedico.model.enums.EstadoConsulta;

import java.time.LocalDateTime;

@Entity
@Table(name = "consulta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Column(length = 5000)
    private String hallazgos;

    @Column(length = 10)
    private String codigoCie10;

    @Column(length = 5000)
    private String diagnostico;

    @Column(length = 5000)
    private String planTratamiento;

    @Column(length = 2000)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConsulta estado;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column
    private LocalDateTime fechaFinalizacion;

    @Version
    private Long version;
}