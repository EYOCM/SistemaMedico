package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoExamenFormDTO {

    // FB PASO 9 - CU-09 INICIO
    @NotBlank(message = "El valor del resultado es obligatorio.")
    @Size(max = 200, message = "El valor del resultado no puede exceder 200 caracteres.")
    private String valorResultado;

    @Size(max = 50, message = "La unidad no puede exceder 50 caracteres.")
    private String unidadResultado;

    @NotNull(message = "La fecha del resultado es obligatoria.")
    @PastOrPresent(message = "La fecha del resultado no puede ser futura.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaResultado;

    // FA02 - CU-09 INICIO
    private boolean fueraDeRango;
    // FA02 - CU-09 FINAL

    @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres.")
    private String notasResultado;
    // FB PASO 9 - CU-09 FINAL
}
