package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenLabDetalleDTO {
    private Long id;
    private String numeroOrden;
    private String nombrePaciente;
    private String dpi;
    private String nombreMedico;
    private String estado;
    private String estadoDescripcion;
    private boolean externa;
    private String observaciones;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;
    private List<ExamenResultadoDTO> examenes;
}
