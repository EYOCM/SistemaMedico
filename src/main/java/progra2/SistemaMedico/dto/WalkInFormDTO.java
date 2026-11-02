package progra2.SistemaMedico.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class WalkInFormDTO {
    private String dpi;
    private Long sucursalId;
    private Long especialidadId;
    private Long medicoId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    private String hora;
    private String motivo;
}
