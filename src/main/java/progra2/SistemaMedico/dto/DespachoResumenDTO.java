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
public class DespachoResumenDTO {
    private Long id;
    private String numeroDespacho;
    private String numeroReceta;
    private String nombrePaciente;
    private String nombreSucursal;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String registradoPor;
    private List<DespachoResumenLineaDTO> lineas;
    private List<String> sustituciones;
    private List<String> alertasStock;
}
