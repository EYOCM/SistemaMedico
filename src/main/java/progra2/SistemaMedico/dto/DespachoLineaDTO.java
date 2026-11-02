package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoLineaDTO {

    private Long recetaDetalleId;

    private String nombreMedicamento;
    private String unidad;
    private String dosis;
    private String frecuencia;
    private String duracion;
    private String indicaciones;
    private BigDecimal precioUnitario;
    private Integer stockDisponible;
    private Integer stockMinimo;

    // FB PASO 6 - CU-11 INICIO
    @Min(value = 0, message = "La cantidad no puede ser negativa.")
    @Max(value = 1000, message = "La cantidad no puede ser mayor a 1000.")
    private Integer cantidad;
    // FB PASO 6 - CU-11 FINAL

    // FA02 - CU-11 INICIO
    private boolean sustituir;

    private Long medicamentoSustitutoId;

    @Size(max = 500, message = "La razón de sustitución no puede exceder 500 caracteres.")
    private String razonSustitucion;
    // FA02 - CU-11 FINAL
}
