package progra2.SistemaMedico.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventarioFilaDTO {
    private Long id;
    private Long medicamentoId;
    private String medicamento;
    private String unidad;
    private boolean controlado;
    private Long sucursalId;
    private String sucursal;
    private int stockActual;
    private int stockMinimo;
    private boolean bajo;
}
