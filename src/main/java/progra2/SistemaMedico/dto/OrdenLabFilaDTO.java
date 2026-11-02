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
public class OrdenLabFilaDTO {
    private Long id;
    private String numeroOrden;
    private String nombrePaciente;
    private String dpi;
    private String nombreMedico;
    private LocalDateTime fechaCreacion;
    private String estado;
    private String estadoDescripcion;
    private boolean externa;
    private int cantidadExamenes;
    private long publicados;
}
