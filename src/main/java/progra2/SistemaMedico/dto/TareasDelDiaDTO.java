package progra2.SistemaMedico.dto;

import java.util.List;

public record TareasDelDiaDTO(String fecha, String fechaTexto, long total, long pendientes, long completadas,
                              List<TareaDTO> tareas) {
}
