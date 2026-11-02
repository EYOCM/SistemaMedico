package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import progra2.SistemaMedico.model.enums.TipoMovimiento;

import java.math.BigDecimal;

@Data
public class MovimientoFormDTO {

    // FA03 - CU-15 INICIO
    @NotNull(message = "Debe seleccionar un medicamento.")
    private Long medicamentoId;

    private Long sucursalId;

    @NotNull(message = "Debe seleccionar el tipo de movimiento.")
    private TipoMovimiento tipo;

    @NotNull(message = "La cantidad debe ser un número entero positivo.")
    @Min(value = 1, message = "La cantidad debe ser un número entero positivo.")
    private Integer cantidad;

    private BigDecimal costoUnitario;

    @Size(max = 50, message = "La referencia no puede exceder los 50 caracteres.")
    private String referencia;
    // FA03 - CU-15 FINAL

    private String motivo;
}
