package progra2.SistemaMedico.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.model.enums.TipoSeguimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroCita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 2000)
    private String  motivoConsulta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus estado;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoPago;

    @Column(length = 50)
    private String numeroTransaccion;

    @Column
    private LocalDateTime fechaPago;

    @Column
    private LocalDateTime fechaReservaExpira;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    private LocalDateTime fechaActualizacion;

    @Column
    private LocalDateTime horaLlegada;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prioridad prioridad;

    @Column(length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoSeguimiento tipoSeguimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_origen_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Consulta consultaOrigen;

    @Column
    private Boolean recordatorioEnviado;

    @Version
    private Long version;

}
