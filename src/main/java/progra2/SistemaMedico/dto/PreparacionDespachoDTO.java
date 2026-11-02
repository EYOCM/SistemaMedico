package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import progra2.SistemaMedico.model.Medicamento;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparacionDespachoDTO {
    private RecetaBusquedaDTO receta;
    private String nombreSucursal;
    private DespachoFormDTO formulario;
    private List<Medicamento> alternativas;
}
