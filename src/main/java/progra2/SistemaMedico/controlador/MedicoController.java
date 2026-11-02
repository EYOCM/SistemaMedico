package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.ConsultaDTO;
import progra2.SistemaMedico.dto.FormularioConsultaDTO;
import progra2.SistemaMedico.dto.OrdenExamenLineaDTO;
import progra2.SistemaMedico.dto.OrdenLaboratorioFormDTO;
import progra2.SistemaMedico.dto.OrdenLaboratorioResumenDTO;
import progra2.SistemaMedico.dto.PreparacionSeguimientoDTO;
import progra2.SistemaMedico.dto.RecetaFilaDTO;
import progra2.SistemaMedico.dto.RecetaFormDTO;
import progra2.SistemaMedico.dto.RecetaResumenDTO;
import progra2.SistemaMedico.dto.SeguimientoFormDTO;
import progra2.SistemaMedico.excepciones.HorarioNoDisponibleException;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Cie10;
import progra2.SistemaMedico.model.enums.EstadoConsulta;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.model.enums.TipoSeguimiento;
import progra2.SistemaMedico.servicios.MedicoService;
import progra2.SistemaMedico.servicios.OrdenLaboratorioService;
import progra2.SistemaMedico.servicios.RecetaService;
import progra2.SistemaMedico.servicios.SeguimientoService;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/medico")
public class MedicoController {
    private static final String MSG_CONCURRENCIA = "La cita fue actualizada por otro usuario; verifique su estado.";

    private final MedicoService medicoService;
    private final RecetaService recetaService;
    private final OrdenLaboratorioService ordenLaboratorioService;
    private final SeguimientoService seguimientoService;

    // FB PASO 1 - CU-08 INICIO
    @GetMapping
    public String panel(Principal principal, Model model) {
        try {
            model.addAttribute("panel", medicoService.obtenerPanel(principal.getName()));
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("panel", null);
        }
        return "medico/panel";
    }
    // FB PASO 1 - CU-08 FINAL

    // FB PASO 2 - CU-08 INICIO
    @PostMapping("/citas/{citaId}/iniciar")
    public String iniciar(@PathVariable Long citaId, @RequestParam int turno,
                          Principal principal, RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = medicoService.iniciar(citaId, principal.getName());
            redirectAttributes.addFlashAttribute("anuncio",
                    "Turno número " + turno + ". Paciente " + cita.getNombrePaciente()
                            + ", favor pasar a consulta médica.");
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", MSG_CONCURRENCIA);
        }
        return "redirect:/medico";
    }
    // FB PASO 2 - CU-08 FINAL

    // FA06 - CU-08 INICIO
    @PostMapping("/citas/{citaId}/no-asistio")
    public String noAsistio(@PathVariable Long citaId, Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = medicoService.marcarNoAsistio(citaId, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Cita #" + cita.getNumeroCita() + " marcada como No Asistió.");
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", MSG_CONCURRENCIA);
        }
        return "redirect:/medico";
    }
    // FA06 - CU-08 FINAL

    // FB PASOS 11-12 - CU-08 INICIO
    @PostMapping("/citas/{citaId}/finalizar")
    public String finalizar(@PathVariable Long citaId, Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = medicoService.finalizarAtencion(citaId, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Atención finalizada para cita #" + cita.getNumeroCita() + ".");
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", MSG_CONCURRENCIA);
        }
        return "redirect:/medico";
    }
    // FB PASOS 11-12 - CU-08 FINAL

    // FB PASOS 3-4 - CU-08 INICIO
    @GetMapping("/citas/{citaId}/consulta")
    public String mostrarConsulta(@PathVariable Long citaId, Principal principal,
                                  Model model, RedirectAttributes redirectAttributes) {
        try {
            FormularioConsultaDTO f = medicoService.prepararFormulario(citaId, principal.getName());
            cargarFormulario(model, f);
            if (!model.containsAttribute("consulta")) {
                model.addAttribute("consulta", f.getConsulta());
            }
            return "medico/consulta";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/medico";
        }
    }
    // FB PASOS 3-4 - CU-08 FINAL

    // FB PASOS 9-10 - CU-08 INICIO
    @PostMapping("/citas/{citaId}/consulta")
    public String guardarConsulta(@PathVariable Long citaId,
                                  @Valid @ModelAttribute("consulta") ConsultaDTO datos,
                                  BindingResult resultado,
                                  Principal principal,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            try {
                cargarFormulario(model, medicoService.prepararFormulario(citaId, principal.getName()));
                return "medico/consulta";
            } catch (ReglasDeNegocioException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/medico";
            }
        }

        try {
            EstadoConsulta guardado = medicoService.guardarConsulta(citaId, datos, principal.getName());
            // FB PASO 10 - CU-08 INICIO
            if (guardado == EstadoConsulta.FINALIZADA) {
                redirectAttributes.addFlashAttribute("success",
                        "La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas.");
                return "redirect:/medico";
            }
            // FB PASO 10 - CU-08 FINAL
            redirectAttributes.addFlashAttribute("success", "Consulta guardada. Puede continuar editándola.");
            return "redirect:/medico/citas/" + citaId + "/consulta";

        } catch (ReglasDeNegocioException e) {
            // FA05 - CU-08 INICIO
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("consulta", datos);
            return "redirect:/medico/citas/" + citaId + "/consulta";
            // FA05 - CU-08 FINAL

        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", MSG_CONCURRENCIA);
            return "redirect:/medico";
        }
    }
    // FB PASOS 9-10 - CU-08 FINAL

    // FB PASO 7 - CU-08 INICIO
    @GetMapping("/cie10")
    @ResponseBody
    public List<Cie10> buscarCie10(@RequestParam(required = false) String q) {
        return medicoService.buscarCie10(q);
    }
    // FB PASO 7 - CU-08 FINAL

    // FA04 - CU-08 INICIO
    @GetMapping("/citas/{citaId}/receta")
    public String mostrarReceta(@PathVariable Long citaId, Principal principal,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("cita", recetaService.citaEvaluada(citaId, principal.getName()));
            recetaService.buscarResumen(citaId).ifPresent(r -> model.addAttribute("resumen", r));
            if (!model.containsAttribute("resumen")) {
                model.addAttribute("medicamentos", recetaService.medicamentosActivos());
                if (!model.containsAttribute("receta")) {
                    model.addAttribute("receta", formularioRecetaVacio());
                }
            }
            return "medico/receta";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/medico";
        }
    }

    @PostMapping("/citas/{citaId}/receta")
    public String generarReceta(@PathVariable Long citaId,
                                @Valid @ModelAttribute("receta") RecetaFormDTO form,
                                BindingResult resultado,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            try {
                model.addAttribute("cita", recetaService.citaEvaluada(citaId, principal.getName()));
                model.addAttribute("medicamentos", recetaService.medicamentosActivos());
                return "medico/receta";
            } catch (ReglasDeNegocioException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/medico";
            }
        }

        try {
            RecetaResumenDTO receta = recetaService.generar(citaId, form, principal.getName());
            String medicamentos = receta.getFilas().stream()
                    .map(RecetaFilaDTO::getNombreMedicamento)
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("success",
                    "Receta médica generada exitosamente. Medicamentos: " + medicamentos
                            + ". El paciente puede adquirirlos en la farmacia de la clínica.");
            return "redirect:/medico";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("receta", form);
            return "redirect:/medico/citas/" + citaId + "/receta";
        }
    }
    // FA04 - CU-08 FINAL

    // FA01 - CU-08 INICIO
    @GetMapping("/citas/{citaId}/orden")
    public String mostrarOrden(@PathVariable Long citaId, Principal principal,
                               Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("cita", ordenLaboratorioService.citaEvaluada(citaId, principal.getName()));
            ordenLaboratorioService.buscarResumen(citaId).ifPresent(o -> model.addAttribute("resumen", o));
            if (!model.containsAttribute("resumen")) {
                model.addAttribute("examenes", ordenLaboratorioService.examenesActivos());
                if (!model.containsAttribute("orden")) {
                    model.addAttribute("orden", new OrdenLaboratorioFormDTO());
                }
            }
            return "medico/orden";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/medico";
        }
    }

    @PostMapping("/citas/{citaId}/orden")
    public String generarOrden(@PathVariable Long citaId,
                               @Valid @ModelAttribute("orden") OrdenLaboratorioFormDTO form,
                               BindingResult resultado,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            try {
                model.addAttribute("cita", ordenLaboratorioService.citaEvaluada(citaId, principal.getName()));
                model.addAttribute("examenes", ordenLaboratorioService.examenesActivos());
                return "medico/orden";
            } catch (ReglasDeNegocioException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/medico";
            }
        }

        try {
            OrdenLaboratorioResumenDTO orden = ordenLaboratorioService.generar(citaId, form, principal.getName());
            String examenes = orden.getExamenes().stream()
                    .map(OrdenExamenLineaDTO::getNombre)
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("success",
                    "Orden de laboratorio generada exitosamente. Número de orden: " + orden.getNumeroOrden()
                            + ". Exámenes: " + examenes + ". El paciente debe dirigirse al área de laboratorio.");
            return "redirect:/medico";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("orden", form);
            return "redirect:/medico/citas/" + citaId + "/orden";
        }
    }
    // FA01 - CU-08 FINAL

    // FA02 - CU-08 INICIO
    // FB PASOS 2-3 - CU-12 INICIO
    @GetMapping("/citas/{citaId}/seguimiento")
    public String mostrarSeguimiento(@PathVariable Long citaId, Principal principal,
                                     Model model, RedirectAttributes redirectAttributes) {
        try {
            SeguimientoFormDTO previo = (SeguimientoFormDTO) model.getAttribute("seguimiento");
            LocalDate fecha = previo != null ? previo.getFecha() : null;
            PreparacionSeguimientoDTO prep = seguimientoService.preparar(citaId, fecha, principal.getName());
            model.addAttribute("prep", prep);
            if (previo == null) {
                model.addAttribute("seguimiento", SeguimientoFormDTO.builder().fecha(prep.getFecha()).build());
            }
            model.addAttribute("tipos", TipoSeguimiento.values());
            model.addAttribute("prioridades", Prioridad.values());
            return "medico/seguimiento";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/medico/citas/" + citaId + "/consulta";
        }
    }
    // FB PASOS 2-3 - CU-12 FINAL

    // FB PASO 5 - CU-12 INICIO
    @GetMapping("/citas/{citaId}/seguimiento/horarios")
    @ResponseBody
    public List<String> horariosSeguimiento(@PathVariable Long citaId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                            Principal principal) {
        return seguimientoService.horarios(citaId, fecha, principal.getName());
    }
    // FB PASO 5 - CU-12 FINAL

    // FB PASOS 6-8 - CU-12 INICIO
    @PostMapping("/citas/{citaId}/seguimiento")
    public String agendarSeguimiento(@PathVariable Long citaId,
                                     @Valid @ModelAttribute("seguimiento") SeguimientoFormDTO form,
                                     BindingResult resultado,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return volverASeguimiento(citaId, form, resultado.getAllErrors().get(0).getDefaultMessage(),
                    redirectAttributes);
        }
        try {
            // FB PASO 8 - CU-12 INICIO
            redirectAttributes.addFlashAttribute("success",
                    seguimientoService.agendar(citaId, form, principal.getName()));
            return "redirect:/medico/citas/" + citaId + "/consulta";
            // FB PASO 8 - CU-12 FINAL
        } catch (HorarioNoDisponibleException e) {
            // FA01 - CU-12 INICIO
            form.setHora(null);
            return volverASeguimiento(citaId, form, e.getMessage(), redirectAttributes);
            // FA01 - CU-12 FINAL
        } catch (ReglasDeNegocioException e) {
            return volverASeguimiento(citaId, form, e.getMessage(), redirectAttributes);
        }
    }
    // FB PASOS 6-8 - CU-12 FINAL
    // FA02 - CU-08 FINAL

    private String volverASeguimiento(Long citaId, SeguimientoFormDTO form, String mensaje,
                                      RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", mensaje);
        redirectAttributes.addFlashAttribute("seguimiento", form);
        return "redirect:/medico/citas/" + citaId + "/seguimiento";
    }

    private void cargarFormulario(Model model, FormularioConsultaDTO f) {
        model.addAttribute("cita", f.getCita());
        model.addAttribute("historialSignos", f.getHistorialSignos());
        model.addAttribute("estados", EstadoConsulta.values());
        model.addAttribute("consultaGuardada", f.isConsultaGuardada());
        model.addAttribute("seguimientoAgendado", f.getSeguimiento());
    }

    private RecetaFormDTO formularioRecetaVacio() {
        List<RecetaFilaDTO> filas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            filas.add(new RecetaFilaDTO());
        }
        return RecetaFormDTO.builder().filas(filas).build();
    }
}
