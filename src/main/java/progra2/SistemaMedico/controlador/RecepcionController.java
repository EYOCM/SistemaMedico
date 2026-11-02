package progra2.SistemaMedico.controlador;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.ApiResponse;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.EmergenciaFormDTO;
import progra2.SistemaMedico.dto.OpcionDTO;
import progra2.SistemaMedico.dto.PreparacionWalkInDTO;
import progra2.SistemaMedico.dto.ReasignacionDTO;
import progra2.SistemaMedico.dto.ResultadoBusquedaCitaDTO;
import progra2.SistemaMedico.dto.WalkInFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.servicios.CitaService;
import progra2.SistemaMedico.servicios.RecepcionService;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recepcion")
public class RecepcionController {
    private final RecepcionService service;
    private final CitaService citaService;


    // FB PASOS 1-2 - CU-05 INICIO
    @GetMapping
    public String inicio() {
        return "recepcion/buscar";
    }
    // FB PASOS 1-2 - CU-05 FINAL

    // FB PASOS 3-5 - CU-05 INICIO
    @GetMapping("/buscar")
    public String buscar(@RequestParam(defaultValue = "numero") String tipo, @RequestParam(required = false) String valor, Model model) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("valor", valor);

        if (valor == null || valor.isBlank()) {
            model.addAttribute("error", "Debe ingresar un número de cita o DPI para buscar.");
            return "recepcion/buscar";
        }

        try {
            ResultadoBusquedaCitaDTO resultado = buscarCitas(tipo, valor);
            model.addAttribute("resultado", resultado);
            model.addAttribute("huella", huella(resultado));
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());   // RN-GLOBAL-001
        }
        return "recepcion/buscar";
    }
    // FB PASOS 3-5 - CU-05 FINAL

    // RNF-021: la pantalla pregunta cada pocos segundos si cambió algo de lo que muestra
    @GetMapping("/estado")
    @ResponseBody
    public String estado(@RequestParam(defaultValue = "numero") String tipo, @RequestParam String valor) {
        try {
            return huella(buscarCitas(tipo, valor));
        } catch (ReglasDeNegocioException e) {
            return "ERROR";
        }
    }

    private ResultadoBusquedaCitaDTO buscarCitas(String tipo, String valor) {
        return "dpi".equals(tipo) ? service.buscarPorDpi(valor) : service.buscarPorNumero(valor);
    }

    // Resume el resultado en un texto: si una cita cambia de estado (o aparece una nueva), el texto cambia
    private String huella(ResultadoBusquedaCitaDTO resultado) {
        String citas = resultado.getCitas() == null ? "" : resultado.getCitas().stream()
                .map(c -> c.getId() + ":" + c.getEstado() + ":" + c.getNombreMedico())
                .collect(Collectors.joining(","));
        return resultado.getTipo().name() + "|" + citas;
    }

    // FB PASOS 6-7 - CU-05 INICIO
    @PostMapping("/citas/{citaId}/llegada")
    public String registrarLlegada(@PathVariable Long citaId,
                                   @RequestParam String tipo,
                                   @RequestParam String valor,
                                   RedirectAttributes redirectAttributes){
        redirectAttributes.addAttribute("tipo", tipo);
        redirectAttributes.addAttribute("valor", valor);

        try {
            CitaDTO cita = service.registrarLlegada(citaId);
            String mensaje;
            // FA08 - CU-05 INICIO
            if("EMERGENCIA".equals(cita.getPrioridad())) {
                mensaje = "Paciente " + cita.getNombrePaciente()
                        + " registrado con prioridad de EMERGENCIA. "
                        + "El paciente debe pasar directamente a toma de signos vitales.";
                        // FA08 - CU-05 FINAL
            }else {
                // FB PASO 7 - CU-05 INICIO
                mensaje = "La llegada del paciente " + cita.getNombrePaciente()
                        + " ha sido registrada exitosamente. "
                        + "El paciente debe pasar a la sala de espera.";
                        // FB PASO 7 - CU-05 FINAL
            }
            redirectAttributes.addFlashAttribute("success", mensaje);
        } catch (ReglasDeNegocioException e ){
            // FA09 - CU-05 INICIO
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        // excepcion de @Version cuando la cita fue actualizada por otra persona
        catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar la llegada. La cita fue actualizada por otro usuario; verifique su estado.");
        }
        // FA09 - CU-05 FINAL
        return  "redirect:/recepcion/buscar";
    }
    // FB PASOS 6-7 - CU-05 FINAL

    // FA04 / FA06 - CU-05 INICIO
    // CU-05 FA04 / FA06: Nueva Cita (Walk-in)
    @GetMapping("/walk-in")
    public String walkIn(@RequestParam String dpi, Principal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            PreparacionWalkInDTO prep = citaService.prepararWalkIn(dpi, principal.getName());
            WalkInFormDTO form = new WalkInFormDTO();
            form.setDpi(prep.dpi());
            form.setSucursalId(prep.sucursalId());
            form.setFecha(LocalDate.now());
            return mostrarWalkIn(prep, form, model);
        } catch (ReglasDeNegocioException e) {
            return volverABusqueda(dpi, e.getMessage(), redirectAttributes);
        }
    }

    @PostMapping("/walk-in")
    public String agendarWalkIn(@ModelAttribute("walkIn") WalkInFormDTO form, Principal principal,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = citaService.agendarWalkIn(form.getDpi(), aSolicitud(form), principal.getName());
            redirectAttributes.addFlashAttribute("success", "Cita " + cita.getNumeroCita() + " agendada para "
                    + cita.getNombrePaciente() + ". El paciente debe pasar a caja para realizar el pago.");
            redirectAttributes.addAttribute("tipo", "numero");
            redirectAttributes.addAttribute("valor", cita.getNumeroCita());
            return "redirect:/recepcion/buscar";
        } catch (ReglasDeNegocioException e) {
            try {
                model.addAttribute("error", e.getMessage());
                return mostrarWalkIn(citaService.prepararWalkIn(form.getDpi(), principal.getName()), form, model);
            } catch (ReglasDeNegocioException sinPaciente) {
                return volverABusqueda(form.getDpi(), sinPaciente.getMessage(), redirectAttributes);
            }
        }
    }

    @GetMapping("/walk-in/especialidades")
    @ResponseBody
    public List<OpcionDTO> especialidadesWalkIn(@RequestParam Long sucursalId) {
        return citaService.opcionesEspecialidades(sucursalId);
    }

    @GetMapping("/walk-in/medicos")
    @ResponseBody
    public List<OpcionDTO> medicosWalkIn(@RequestParam Long sucursalId, @RequestParam Long especialidadId) {
        return citaService.opcionesMedicos(sucursalId, especialidadId);
    }

    @GetMapping("/walk-in/horarios")
    @ResponseBody
    public List<String> horariosWalkIn(@RequestParam Long medicoId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaService.obtenerHorariosDisponibles(medicoId, fecha);
    }

    private String mostrarWalkIn(PreparacionWalkInDTO prep, WalkInFormDTO form, Model model) {
        model.addAttribute("prep", prep);
        model.addAttribute("walkIn", form);
        model.addAttribute("sucursales", citaService.obtenerSucursalesActivas());
        model.addAttribute("hoy", LocalDate.now());
        return "recepcion/walk-in";
    }

    private String volverABusqueda(String dpi, String mensaje, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", mensaje);
        redirectAttributes.addAttribute("tipo", "dpi");
        redirectAttributes.addAttribute("valor", dpi);
        return "redirect:/recepcion/buscar";
    }

    private ApiResponse.SolicitudCitaDTO aSolicitud(WalkInFormDTO form) {
        ApiResponse.SolicitudCitaDTO solicitud = new ApiResponse.SolicitudCitaDTO();
        solicitud.setSucursalId(form.getSucursalId());
        solicitud.setEspecialidadId(form.getEspecialidadId());
        solicitud.setMedicoId(form.getMedicoId());
        solicitud.setFechaHora(fechaHora(form));
        solicitud.setMotivoCita(form.getMotivo());
        return solicitud;
    }

    private LocalDateTime fechaHora(WalkInFormDTO form) {
        if (form.getFecha() == null || form.getHora() == null || form.getHora().isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.of(form.getFecha(), LocalTime.parse(form.getHora()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    // FA04 / FA06 - CU-05 FINAL

    // FA07 - CU-05 INICIO
    // CU-05 FA07: Reasignar médico
    @GetMapping("/citas/{citaId}/reasignar")
    public String reasignar(@PathVariable Long citaId,
                            @RequestParam(defaultValue = "numero") String tipo,
                            @RequestParam(required = false) String valor,
                            Model model, RedirectAttributes redirectAttributes) {
        try {
            ReasignacionDTO reasignacion = citaService.prepararReasignacion(citaId);
            model.addAttribute("reasignacion", reasignacion);
            model.addAttribute("tipo", tipo);
            model.addAttribute("valor", valor == null ? reasignacion.cita().getNumeroCita() : valor);
            return "recepcion/reasignar";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("tipo", tipo);
            redirectAttributes.addAttribute("valor", valor);
            return "redirect:/recepcion/buscar";
        }
    }

    @PostMapping("/citas/{citaId}/reasignar")
    public String confirmarReasignacion(@PathVariable Long citaId,
                                        @RequestParam(required = false) Long medicoId,
                                        @RequestParam(required = false) String nota,
                                        @RequestParam(defaultValue = "numero") String tipo,
                                        @RequestParam(required = false) String valor,
                                        Principal principal, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("tipo", tipo);
        redirectAttributes.addAttribute("valor", valor);
        try {
            citaService.reasignarMedico(citaId, medicoId, nota, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Médico reasignado correctamente");
            return "redirect:/recepcion/buscar";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("notaPrevia", nota);
            return "redirect:/recepcion/citas/{citaId}/reasignar";
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                    "La cita fue actualizada por otro usuario; verifique su estado e intente de nuevo.");
            return "redirect:/recepcion/buscar";
        }
    }
    // FA07 - CU-05 FINAL

    // FA01 - CU-05 INICIO
    // CU-05 FA01: paciente que llega por emergencia sin cita
    @GetMapping("/emergencia")
    public String emergencia(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        try {
            return mostrarEmergencia(new EmergenciaFormDTO(), principal.getName(), model);
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/recepcion";
        }
    }

    @PostMapping("/emergencia")
    public String registrarEmergencia(@ModelAttribute("emergencia") EmergenciaFormDTO form, Principal principal,
                                      Model model, RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = citaService.registrarEmergencia(form, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Paciente " + cita.getNombrePaciente()
                    + " registrado con prioridad de EMERGENCIA. El paciente debe pasar directamente a toma de signos vitales.");
            redirectAttributes.addAttribute("tipo", "numero");
            redirectAttributes.addAttribute("valor", cita.getNumeroCita());
            return "redirect:/recepcion/buscar";
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return mostrarEmergencia(form, principal.getName(), model);
        }
    }

    private String mostrarEmergencia(EmergenciaFormDTO form, String recepcionista, Model model) {
        model.addAttribute("sede", citaService.sedeDe(recepcionista));
        model.addAttribute("emergencia", form);
        return "recepcion/emergencia";
    }
    // FA01 - CU-05 FINAL
}
