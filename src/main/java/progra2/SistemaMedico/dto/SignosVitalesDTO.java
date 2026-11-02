package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignosVitalesDTO {

    private static final String MSG_PRESION =
            "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos.";
    private static final String MSG_TEMP = "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.";
    private static final String MSG_PESO = "El peso debe estar entre 0.5 y 300 kg con dos decimales.";
    private static final String MSG_TALLA = "La talla debe estar entre 30 y 250 cm con dos decimales.";
    private static final String MSG_FC = "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.";

    // FA02 - CU-07 INICIO
    @NotNull(message = "La presión arterial es obligatoria.")
    @Min(value = 60, message = MSG_PRESION)
    @Max(value = 250, message = MSG_PRESION)
    private Integer sistolica;

    @NotNull(message = "La presión arterial es obligatoria.")
    @Min(value = 40, message = MSG_PRESION)
    @Max(value = 150, message = MSG_PRESION)
    private Integer diastolica;

    @NotNull(message = "La temperatura es obligatoria.")
    @DecimalMin(value = "34.0", message = MSG_TEMP)
    @DecimalMax(value = "42.0", message = MSG_TEMP)
    @Digits(integer = 2, fraction = 1, message = MSG_TEMP)
    private BigDecimal temperatura;

    @NotNull(message = "El peso es obligatorio.")
    @DecimalMin(value = "0.5", message = MSG_PESO)
    @DecimalMax(value = "300", message = MSG_PESO)
    @Digits(integer = 3, fraction = 2, message = MSG_PESO)
    private BigDecimal peso;

    @NotNull(message = "La talla es obligatoria.")
    @DecimalMin(value = "30", message = MSG_TALLA)
    @DecimalMax(value = "250", message = MSG_TALLA)
    @Digits(integer = 3, fraction = 2, message = MSG_TALLA)
    private BigDecimal talla;

    @NotNull(message = "La frecuencia cardíaca es obligatoria.")
    @Min(value = 30, message = MSG_FC)
    @Max(value = 220, message = MSG_FC)
    private Integer frecuenciaCardiaca;
    // FA02 - CU-07 FINAL

    // FA01 - CU-07 INICIO
    private boolean emergencia;
    // FA01 - CU-07 FINAL
}
