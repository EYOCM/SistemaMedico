package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.Size;
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
public class OrdenLaboratorioFormDTO {

    @Builder.Default
    private List<Long> examenIds = new ArrayList<>();

    // FA01 - CU-09 INICIO
    private boolean externa;
    // FA01 - CU-09 FINAL

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres.")
    private String observaciones;
}
