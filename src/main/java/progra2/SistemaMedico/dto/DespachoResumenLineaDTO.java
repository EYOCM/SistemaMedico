package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoResumenLineaDTO {
    private String nombre;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String sustituyeA;
    private String razonSustitucion;
}
