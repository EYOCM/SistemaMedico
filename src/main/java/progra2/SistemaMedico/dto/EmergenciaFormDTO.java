package progra2.SistemaMedico.dto;

import lombok.Data;

@Data
public class EmergenciaFormDTO {
    private String dpi;
    private String nombre;
    private String apellidos;
    private Long especialidadId;
    private Long medicoId;
    private String motivo;
}
