package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import progra2.SistemaMedico.model.SignosVitales;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormularioConsultaDTO {
    private CitaDTO cita;
    private ConsultaDTO consulta;
    private List<SignosVitales> historialSignos;
    private boolean consultaGuardada;
    private CitaDTO seguimiento;
}