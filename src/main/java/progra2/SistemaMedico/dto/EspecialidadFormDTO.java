package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadFormDTO {

    private Long id;

    // FA05 - CU-14 INICIO
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres.")
    private String descripcion;

    @NotNull(message = "El precio de la consulta es obligatorio.")
    @DecimalMin(value = "0.01", message = "El precio de la consulta debe ser mayor a 0.")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales.")
    private BigDecimal precioConsulta;

    @NotNull(message = "El estado es obligatorio.")
    @Builder.Default
    private Boolean activo = true;
    // FA05 - CU-14 FINAL
}
