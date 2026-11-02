package progra2.SistemaMedico.dto;

import java.util.Map;

public record RespuestaAgendaDTO(boolean ok, String mensaje, String aviso, Map<String, String> errores) {

    public static RespuestaAgendaDTO exito(String mensaje) {
        return new RespuestaAgendaDTO(true, mensaje, null, Map.of());
    }

    public static RespuestaAgendaDTO exito(String mensaje, String aviso) {
        return new RespuestaAgendaDTO(true, mensaje, aviso, Map.of());
    }

    public static RespuestaAgendaDTO error(String mensaje) {
        return new RespuestaAgendaDTO(false, mensaje, null, Map.of());
    }

    public static RespuestaAgendaDTO errores(Map<String, String> errores) {
        return new RespuestaAgendaDTO(false, "Revise los campos marcados en rojo.", null, errores);
    }
}
