package progra2.SistemaMedico.dto;

public record TareaDTO(Long id, String titulo, String descripcion, String prioridad, String prioridadDescripcion,
                       String color, String fechaLimite, boolean completada) {
}
