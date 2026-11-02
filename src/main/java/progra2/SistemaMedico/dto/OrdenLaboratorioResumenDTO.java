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
public class OrdenLaboratorioResumenDTO {
    private String numeroOrden;
    private String estado;
    private boolean externa;
    private String observaciones;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;
    private List<OrdenExamenLineaDTO> examenes;
}
