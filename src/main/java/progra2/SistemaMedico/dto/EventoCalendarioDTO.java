package progra2.SistemaMedico.dto;

import java.util.Map;

public record EventoCalendarioDTO(String id, String title, String start, String end, boolean allDay,
                                  String color, Map<String, Object> extendedProps) {
}
