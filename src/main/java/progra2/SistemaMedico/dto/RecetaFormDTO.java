package progra2.SistemaMedico.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaFormDTO {

    @Valid
    @Builder.Default
    private List<RecetaFilaDTO> filas = new ArrayList<>();
}
