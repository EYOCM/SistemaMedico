package progra2.SistemaMedico.controlador;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.PrioridadTarea;
import progra2.SistemaMedico.model.enums.TipoEvento;
import progra2.SistemaMedico.servicios.AgendaService;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/medico/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    // FB PASO 1 - CU-16 INICIO
    // Paso 1: el médico entra a "Agenda Médica"; el calendario pide los datos por JSON
    @GetMapping
    public String agenda(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        try {
            Usuario medico = agendaService.medico(principal.getName());
            model.addAttribute("nombreMedico", medico.getNombre() + " " + medico.getApellidos());
            model.addAttribute("tipos", TipoEvento.values());
            model.addAttribute("prioridades", PrioridadTarea.values());
            model.addAttribute("hoy", LocalDate.now());
            model.addAttribute("colorEvento", AgendaService.COLOR_EVENTO);
            return "medico/agenda";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/medico";
        }
    }
    // FB PASO 1 - CU-16 FINAL

    // FB PASO 2 / FA05 - CU-16 INICIO
    @GetMapping("/eventos")
    @ResponseBody
    public List<EventoCalendarioDTO> eventos(@RequestParam String start, @RequestParam String end, Principal principal) {
        return agendaService.calendario(principal.getName(), limite(start), limite(end));
    }
    // FB PASO 2 / FA05 - CU-16 FINAL

    // FB PASOS 6-8 / FA07 - CU-16 INICIO
    @PostMapping("/eventos")
    @ResponseBody
    public ResponseEntity<RespuestaAgendaDTO> guardarEvento(@RequestBody EventoFormDTO form, Principal principal) {
        return responder(agendaService.guardarEvento(form, principal.getName()));
    }
    // FB PASOS 6-8 / FA07 - CU-16 FINAL

    // FA04 - CU-16 INICIO
    @PostMapping("/eventos/{id}/eliminar")
    @ResponseBody
    public ResponseEntity<RespuestaAgendaDTO> eliminarEvento(@PathVariable Long id, Principal principal) {
        return responder(agendaService.eliminarEvento(id, principal.getName()));
    }
    // FA04 - CU-16 FINAL

    // FA06 - CU-16 INICIO
    @GetMapping("/tareas")
    @ResponseBody
    public TareasDelDiaDTO tareas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  @RequestParam(defaultValue = "pendientes") String filtro,
                                  Principal principal) {
        return agendaService.tareas(principal.getName(), fecha, filtro);
    }

    @PostMapping("/tareas")
    @ResponseBody
    public ResponseEntity<RespuestaAgendaDTO> crearTarea(@RequestBody TareaFormDTO form, Principal principal) {
        return responder(agendaService.crearTarea(form, principal.getName()));
    }

    @PostMapping("/tareas/{id}/estado")
    @ResponseBody
    public ResponseEntity<RespuestaAgendaDTO> cambiarEstadoTarea(@PathVariable Long id, Principal principal) {
        return responder(agendaService.cambiarEstadoTarea(id, principal.getName()));
    }

    @PostMapping("/tareas/{id}/eliminar")
    @ResponseBody
    public ResponseEntity<RespuestaAgendaDTO> eliminarTarea(@PathVariable Long id, Principal principal) {
        return responder(agendaService.eliminarTarea(id, principal.getName()));
    }
    // FA06 - CU-16 FINAL

    // FA03 - CU-16 INICIO
    @ExceptionHandler(ReglasDeNegocioException.class)
    @ResponseBody
    public ResponseEntity<RespuestaAgendaDTO> reglaDeNegocio(ReglasDeNegocioException e) {
        return ResponseEntity.badRequest().body(RespuestaAgendaDTO.error(e.getMessage()));
    }
    // FA03 - CU-16 FINAL

    private ResponseEntity<RespuestaAgendaDTO> responder(RespuestaAgendaDTO respuesta) {
        return respuesta.ok() ? ResponseEntity.ok(respuesta) : ResponseEntity.badRequest().body(respuesta);
    }

    // FullCalendar manda "2026-09-28T00:00:00-06:00" o "2026-09-28": se toma la fecha y hora local
    private LocalDateTime limite(String valor) {
        return valor.length() <= 10 ? LocalDate.parse(valor).atStartOfDay() : LocalDateTime.parse(valor.substring(0, 19));
    }
}
