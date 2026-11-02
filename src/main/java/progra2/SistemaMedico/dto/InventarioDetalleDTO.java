package progra2.SistemaMedico.dto;

import java.util.List;

public record InventarioDetalleDTO(InventarioFilaDTO inventario, String mes, List<ResumenTipoDTO> resumen,
                                   long neto, List<MovimientoFilaDTO> ultimos) {
}
