package progra2.SistemaMedico.dto;

import lombok.Data;

@Data
public class EventoFormDTO {
    // FB PASOS 4-5 - CU-16 INICIO
    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private boolean todoElDia;
    private String inicio;
    private String fin;
    // FB PASOS 4-5 - CU-16 FINAL
}
