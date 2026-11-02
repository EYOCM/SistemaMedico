package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import progra2.SistemaMedico.model.enums.MetodoPago;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CobroCajaDTO {
    private MetodoPago metodo;
    private BigDecimal montoRecibido;
    private String ultimosDigitos;
}
