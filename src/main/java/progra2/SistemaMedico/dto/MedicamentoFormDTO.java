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
public class MedicamentoFormDTO {

    private Long id;

    // FA05 - CU-14 INICIO
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres.")
    private String descripcion;

    @NotNull(message = "El precio debe ser mayor a 0.")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0.")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales.")
    private BigDecimal precio;

    @NotBlank(message = "La unidad es obligatoria.")
    @Size(max = 50, message = "La unidad no puede exceder 50 caracteres.")
    private String unidad;

    private boolean controlado;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
    private Integer stockMinimo;

    @NotNull(message = "El estado es obligatorio.")
    @Builder.Default
    private Boolean activo = true;
    // FA05 - CU-14 FINAL
}
