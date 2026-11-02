package progra2.SistemaMedico.controlador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import progra2.SistemaMedico.dto.ApiResponse;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.EspecialidadDTO;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.servicios.serviceImpl.ICitaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

    private final ICitaService citaService;
    private static final String MENSAJE_RESERVA_EXPIRADA =
            "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. "
                    + "Por favor, seleccione un nuevo horario.";
/* uso de sessions para el asistente de 5 pasos
* Los datos viven en el servidor, asociados a ese navegador.
* El cliente no los ve ni los puede tocar. Se borran solos cuando la sesión expira.
* */
    // FB PASOS 1-2 - CU-03 INICIO
    // PASO 1: SUCURSAL aca se remueven los atributos en caso haya sucedido algo y se inicia el proceso
    @GetMapping("/paso/1")
    public String paso1Sucursal(Model model, HttpSession session) {
        session.removeAttribute("sucursalId");
        session.removeAttribute("especialidadId");
        session.removeAttribute("medicoId");
        session.removeAttribute("fechaHora");

        model.addAttribute("sucursales", citaService.obtenerSucursalesActivas());
        return "citas/paso-1";
    }

    @PostMapping("/paso/1")
    public String procesarPaso1(@RequestParam Long sucursalId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (sucursalId == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una sucursal");
            return "redirect:/citas/paso/1";
        }
        session.setAttribute("sucursalId", sucursalId);
        return "redirect:/citas/paso/2";
    }
    // FB PASOS 1-2 - CU-03 FINAL

    // FB PASO 3 - CU-03 INICIO
    // --- PASO 2: ESPECIALIDAD ---
    @GetMapping("/paso/2")
    public String paso2Especialidad(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long sucursalId = (Long) session.getAttribute("sucursalId"); // conversion del session a un Long
        if (sucursalId == null) return "redirect:/citas/paso/1"; // rebote al paso 1

        List<EspecialidadDTO> especialidades = citaService.obtenerEspecialidadesPorSucursal(sucursalId);
        // FA01 - CU-03 INICIO
        if (especialidades.isEmpty()) {

            String nombreSucursal = citaService.obtenerSucursalesActivas().stream()
                    .filter(su -> su.getId().equals(sucursalId))
                    .map(su -> su.getNombre())
                    .findFirst().orElse("seleccionada");
            redirectAttributes.addFlashAttribute("error",
                    "No hay especialidades disponibles para la sucursal " + nombreSucursal + ". Seleccione otra sucursal.");
            return "redirect:/citas/paso/1";
        }
        // FA01 - CU-03 FINAL
        model.addAttribute("especialidades", especialidades);
        return "citas/paso-2";
    }

    @PostMapping("/paso/2")
    public String procesarPaso2(@RequestParam Long especialidadId, HttpSession session) { // el objeto session es la memoria del asistente
        session.setAttribute("especialidadId", especialidadId);
        return "redirect:/citas/paso/3";
    }
    // FB PASO 3 - CU-03 FINAL

    // FB PASO 4 - CU-03 INICIO
    // --- PASO 3: MÉDICO ---
    @GetMapping("/paso/3")
    public String paso3Medico(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long sucursalId = (Long) session.getAttribute("sucursalId");
        Long especialidadId = (Long) session.getAttribute("especialidadId");

        if (sucursalId == null || especialidadId == null) return "redirect:/citas/paso/1";

        List<UsuarioDTO> medicos = citaService.obtenerMedicosDisponibles(sucursalId, especialidadId);
        // FA02 - CU-03 INICIO
        if (medicos.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "No se encontraron horarios disponibles para la especialidad seleccionada en esta sede. "
                  + "Por favor, seleccione otra especialidad o sede.");
            return "redirect:/citas/paso/2";
        }
        // FA02 - CU-03 FINAL
        model.addAttribute("medicos", medicos);
        return "citas/paso-3";
    }

    @PostMapping("/paso/3")
    public String procesarPaso3(@RequestParam Long medicoId, HttpSession session) {
        session.setAttribute("medicoId", medicoId);
        return "redirect:/citas/paso/4";
    }
    // FB PASO 4 - CU-03 FINAL

    // FB PASO 5 - CU-03 INICIO
    // --- PASO 4: FECHA Y HORA ---

    @GetMapping("/paso/4")
    public String paso4FechaHora(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                 HttpSession session, Model model) {
        Long medicoId = (Long) session.getAttribute("medicoId");
        if (medicoId == null) return "redirect:/citas/paso/1";

        // fecha dia siguiente.
        if (fecha == null || fecha.isBefore(LocalDate.now())) {
            fecha = LocalDate.now().plusDays(1);
        }

        List<String> horarios = citaService.obtenerHorariosDisponibles(medicoId, fecha);

        // FA02 - CU-03 INICIO
        // sin horarios no se redirige se avisa y se deja elegir otro día,
        // que es lo que el paciente realmente quiere hacer.
        if (horarios.isEmpty()) {
            model.addAttribute("error",
                    "No se encontraron horarios disponibles para el médico seleccionado en esa fecha. "
                  + "Por favor, elija otro día.");
        }
        // FA02 - CU-03 FINAL

        model.addAttribute("fecha", fecha);
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("horarios", horarios);
        return "citas/paso-4";
    }

    @PostMapping("/paso/4")
    public String procesarPaso4(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                @RequestParam String hora,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime fechaHora = LocalDateTime.of(fecha, LocalTime.parse(hora));
            session.setAttribute("fechaHora", fechaHora);
            return "redirect:/citas/paso/5";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fecha u Hora inválida");
            return "redirect:/citas/paso/4";
        }
    }
    // FB PASO 5 - CU-03 FINAL

    // FB PASO 6 - CU-03 INICIO
    // --- PASO 5: CONFIRMACIÓN ---
    @GetMapping("/paso/5")
    public String paso5Confirmacion(HttpSession session, Model model) {
        Long sucursalId = (Long) session.getAttribute("sucursalId");
        Long especialidadId = (Long) session.getAttribute("especialidadId");
        Long medicoId = (Long) session.getAttribute("medicoId");
        LocalDateTime fechaHora = (LocalDateTime) session.getAttribute("fechaHora");

        if (sucursalId == null || especialidadId == null || medicoId == null || fechaHora == null) {
            return "redirect:/citas/paso/1";
        }

        // Obtener nombres para el resumen
        String sucursalNombre = citaService.obtenerSucursalesActivas().stream()
                .filter(s -> s.getId().equals(sucursalId)).map(s -> s.getNombre()).findFirst().orElse("Desconocida");
        String especialidadNombre = citaService.obtenerEspecialidadesPorSucursal(sucursalId).stream()
                .filter(e -> e.getId().equals(especialidadId)).map(e -> e.getNombre()).findFirst().orElse("Desconocida");
        String medicoNombre = citaService.obtenerMedicosDisponibles(sucursalId, especialidadId).stream()
                .filter(m -> m.getId().equals(medicoId)).map(m -> m.getNombre() + " " + m.getApellidos()).findFirst().orElse("Desconocido");

        model.addAttribute("sucursalNombre", sucursalNombre);
        model.addAttribute("especialidadNombre", especialidadNombre);
        model.addAttribute("medicoNombre", medicoNombre);
        model.addAttribute("fechaHora", fechaHora);

        return "citas/paso-5";
    }
    // FB PASO 6 - CU-03 FINAL

    // FB PASOS 7-10 - CU-03 INICIO
    @PostMapping("/paso/5")
    public String procesarPaso5(@RequestParam String motivoConsulta, HttpSession session, RedirectAttributes redirectAttributes) {
        // FB PASO 7 - CU-03 INICIO
        // motivo entre 10 y 2000 caracteres
        String motivo = motivoConsulta == null ? "" : motivoConsulta.trim();
        if (motivo.length() < 10 || motivo.length() > 2000) {
            redirectAttributes.addFlashAttribute("error",
                    "El motivo debe contener entre 10 y 2000 caracteres. Usted ingresó " + motivo.length() + " caracteres.");
            redirectAttributes.addFlashAttribute("motivoPrevio", motivo);
            return "redirect:/citas/paso/5";
        }
        // FB PASO 7 - CU-03 FINAL

        ApiResponse.SolicitudCitaDTO solicitud = new ApiResponse.SolicitudCitaDTO();
        solicitud.setSucursalId((Long) session.getAttribute("sucursalId"));
        solicitud.setEspecialidadId((Long) session.getAttribute("especialidadId"));
        solicitud.setMedicoId((Long) session.getAttribute("medicoId"));
        solicitud.setFechaHora((LocalDateTime) session.getAttribute("fechaHora"));
        solicitud.setMotivoCita(motivo);

        try {
            // FB PASOS 9-10 - CU-03 INICIO
            CitaDTO citaCreada = citaService.agendarCita(solicitud);

            session.removeAttribute("sucursalId");
            session.removeAttribute("especialidadId");
            session.removeAttribute("medicoId");
            session.removeAttribute("fechaHora");

            redirectAttributes.addFlashAttribute("success",
                    "Su cita ha sido registrada exitosamente. Será redirigido al proceso de pago para confirmar la reserva.");
            return "redirect:/pago/" + citaCreada.getId();   // paso 10 -> CU-04
            // FB PASOS 9-10 - CU-03 FINAL

        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("motivoPrevio", motivo);
            return "redirect:/citas/paso/5";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo registrar la cita. Intente de nuevo más tarde.");
            return "redirect:/citas/paso/5";
        }
    }
    // FB PASOS 7-10 - CU-03 FINAL

    // FA04 - CU-03 INICIO
    // --- VOLVER ATRÁS ---
    @GetMapping("/volver/{pasoAnterior}")
    public String volver(@PathVariable int pasoAnterior, HttpSession session) {
        if (pasoAnterior == 1) {
            session.removeAttribute("sucursalId");
            session.removeAttribute("especialidadId");
            session.removeAttribute("medicoId");
            session.removeAttribute("fechaHora");
        } else if (pasoAnterior == 2) {
            session.removeAttribute("especialidadId");
            session.removeAttribute("medicoId");
            session.removeAttribute("fechaHora");
        } else if (pasoAnterior == 3) {
            session.removeAttribute("medicoId");
            session.removeAttribute("fechaHora");
        } else if (pasoAnterior == 4) {
            session.removeAttribute("fechaHora");
        }
        return "redirect:/citas/paso/" + pasoAnterior;
    }
    // FA04 - CU-03 FINAL

    // FA03 - CU-03 INICIO
    // FA02 - CU-04 INICIO
    // para tomar los datos de la sesion vencida y enviar al paso 4 del wizard

    @GetMapping("/reintentar/{citaId}")
    public String reintentar(@PathVariable Long citaId, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = citaService.obtenerPorId(citaId);
            session.setAttribute("sucursalId", cita.getSucursalId());
            session.setAttribute("especialidadId", cita.getEspecialidadId());
            session.setAttribute("medicoId", cita.getMedicoId());
            session.removeAttribute("fechaHora");
            redirectAttributes.addFlashAttribute("error", MENSAJE_RESERVA_EXPIRADA);
            return "redirect:/citas/paso/4?fecha=" + cita.getFechaHora().toLocalDate();
        } catch (ReglasDeNegocioException e) {
            return "redirect:/citas/paso/1";
        }
    }
    // FA03 - CU-03 FINAL
    // FA02 - CU-04 FINAL
}

