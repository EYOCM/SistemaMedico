package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.model.enums.TipoSeguimiento;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoFormDTO {

    // FB PASO 4 - CU-12 INICIO
    @NotNull(message = "Debe seleccionar el tipo de seguimiento.")
    private TipoSeguimiento tipo;
    // FB PASO 4 - CU-12 FINAL

    // FB PASO 5 - CU-12 INICIO
    @NotNull(message = "Seleccione una fecha futura dentro de los horarios disponibles del médico.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotBlank(message = "Seleccione una fecha futura dentro de los horarios disponibles del médico.")
    private String hora;
    // FB PASO 5 - CU-12 FINAL

    // FB PASO 6 - CU-12 INICIO
    @NotBlank(message = "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.")
    @Size(min = 10, max = 2000, message = "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.")
    private String observaciones;

    @NotNull(message = "Seleccione la prioridad del seguimiento.")
    @Builder.Default
    private Prioridad prioridad = Prioridad.NORMAL;
    // FB PASO 6 - CU-12 FINAL
}
