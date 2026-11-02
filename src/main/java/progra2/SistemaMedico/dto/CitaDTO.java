package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaDTO {
    private Long id;
    private String numeroCita;
    private String nombreSucursal;
    private String nombreEspecialidad;
    private String nombreMedico;
    private String nombrePaciente;
    private LocalDateTime fechaHora;
    private String motivo;
    private String estado;
    private String estadoDescripcion;
    private BigDecimal monto;
    private BigDecimal montoPago;
    private long segundosRestantes;
    private String numeroTransaccion;
    private LocalDateTime fechaPago;
    private LocalDateTime horaLlegada;
    private String prioridad;
    private String tipoSeguimiento;
    private boolean reservaSinVencimiento;
    // para tomar los datos de la sesion vencida y enviar al paso 4 del wizard
    private Long sucursalId;
    private Long especialidadId;
    private Long medicoId;
    private String dpiPaciente;
}
