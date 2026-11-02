package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparacionSeguimientoDTO {
    private Long citaId;
    private String numeroCita;
    private Long consultaId;
    private String nombrePaciente;
    private String emailPaciente;
    private String nombreMedico;
    private String nombreEspecialidad;
    private String nombreSucursal;
    private LocalDate fecha;
    private LocalDate primerDia;
    private List<String> horarios;
}
