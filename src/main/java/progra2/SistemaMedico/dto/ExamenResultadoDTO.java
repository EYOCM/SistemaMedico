package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamenResultadoDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private String rangoReferencia;
    private String unidadCatalogo;
    private String valorResultado;
    private String unidadResultado;
    private LocalDateTime fechaResultado;
    private boolean fueraDeRango;
    private String notasResultado;
    private boolean publicado;
    private LocalDateTime fechaPublicacion;
    private Boolean sugerenciaFueraDeRango;
}
