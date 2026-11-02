package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaBusquedaDTO {
    private Long id;
    private String numeroReceta;
    private String numeroCita;
    private String nombrePaciente;
    private String dpi;
    private String nombreMedico;
    private LocalDateTime fechaEmision;
    private long diasTranscurridos;
    private boolean vigente;
    private int cantidadMedicamentos;
}
