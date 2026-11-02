package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteLaboratorioDTO {
    private OrdenCobroDTO orden;
    private String metodo;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private String ultimosDigitos;
    private String numeroTransaccion;
    private LocalDateTime fechaPago;
    private String registradoPor;
}
