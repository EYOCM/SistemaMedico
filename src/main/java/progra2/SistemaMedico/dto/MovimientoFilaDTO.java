package progra2.SistemaMedico.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MovimientoFilaDTO {
    private Long id;
    private String tipo;
    private String tipoDescripcion;
    private boolean entrada;
    private Long medicamentoId;
    private String medicamento;
    private Long sucursalId;
    private String sucursal;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private BigDecimal costoUnitario;
    private String etiquetaReferencia;
    private String referencia;
    private String etiquetaMotivo;
    private String motivo;
    private String usuario;
    private LocalDateTime fecha;
    private boolean activo;
    private boolean manual;
}
