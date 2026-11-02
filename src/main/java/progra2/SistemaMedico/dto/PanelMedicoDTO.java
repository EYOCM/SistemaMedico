package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelMedicoDTO {
    private String nombreMedico;
    private List<CitaDTO> enEspera;
    private List<CitaDTO> enConsulta;
    private List<CitaDTO> evaluados;
    private Set<Long> citasConReceta;
    private Set<Long> citasConOrden;


}
