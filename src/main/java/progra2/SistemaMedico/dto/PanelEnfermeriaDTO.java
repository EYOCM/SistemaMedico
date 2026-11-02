package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelEnfermeriaDTO {
    private String nombreSede; // titulo del panel
    private List<CitaDTO> enSala; // esperando a ser llamados
    private List<CitaDTO> enProceso; //Ya llamados, con el botón "Registrar Signos Vitales"
}