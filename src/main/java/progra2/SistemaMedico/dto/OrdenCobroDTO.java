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
public class OrdenCobroDTO {
    private Long id;
    private String numeroOrden;
    private String nombrePaciente;
    private String dpi;
    private String nombreMedico;
    private LocalDateTime fechaCreacion;
    private BigDecimal total;
    private List<OrdenExamenLineaDTO> examenes;
}
