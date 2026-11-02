package progra2.SistemaMedico.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import progra2.SistemaMedico.model.enums.TipoMovimiento;

import java.time.LocalDate;

@Data
public class FiltroMovimientoDTO {
    // FB PASO 2 - CU-15 INICIO
    private TipoMovimiento tipo;
    private Long medicamentoId;
    private Long sucursalId;
    private String referencia;
    private String usuario;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;

    private boolean inactivos;
    // FB PASO 2 - CU-15 FINAL
}
