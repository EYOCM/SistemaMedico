package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.PagoDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.servicios.CitaService;
import progra2.SistemaMedico.servicios.PagoService;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PagoController {

    private final CitaService citaService;
    private final PagoService pagoService;


    private static final DateTimeFormatter FORMATO_VENCIMIENTO = DateTimeFormatter.ofPattern("MM/yy");


    // FB PASOS 1-2 - CU-04 INICIO
    @GetMapping("/pago/{citaId}")
    public String mostrarPago(@PathVariable Long citaId, Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = citaService.obtenerPorId(citaId);

            // FA02 - CU-04 INICIO
            String salida = verificarCitaPagable(cita);
            if (salida != null) {
                return salida;
            }
            // FA02 - CU-04 FINAL

            model.addAttribute("cita", cita);
            model.addAttribute("pago", new PagoDTO());
            model.addAttribute("claveIdempotencia", UUID.randomUUID().toString());
            return "pago/pago";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la cita solicitada.");
            return "redirect:/paciente/dashboard";
        }
    }
    // FB PASOS 1-2 - CU-04 FINAL


    // FB PASOS 7-14 - CU-04 INICIO
    @PostMapping("/pago/{citaId}")
    public String procesarPago(@PathVariable Long citaId,
                               @Valid @ModelAttribute("pago") PagoDTO pago,
                               BindingResult resultado,
                               Model model,
                               RedirectAttributes redirectAttributes, @RequestParam(required = false) String claveIdempotencia) {
        try {

            CitaDTO cita = citaService.obtenerPorId(citaId);

            // FA02 - CU-04 INICIO
            String salida = verificarCitaPagable(cita);
            if (salida != null) {
                return salida;
            }
            // FA02 - CU-04 FINAL


            // FA01 - CU-04 INICIO
            if (!resultado.hasFieldErrors("vencimiento")) {
                YearMonth vence = YearMonth.parse(pago.getVencimiento(), FORMATO_VENCIMIENTO);
                if (vence.isBefore(YearMonth.now())) {
                    resultado.rejectValue("vencimiento", "vencida", "La tarjeta está vencida.");
                }
            }


            if (resultado.hasErrors()) {
                pago.setCvv(null);
                model.addAttribute("cita", cita);
                model.addAttribute("claveIdempotencia", claveIdempotencia);
                return "pago/pago";
            }
            // FA01 - CU-04 FINAL

            try {
                // FB PASOS 10-14 - CU-04 INICIO
                pagoService.procesarPago(citaId, pago, claveIdempotencia);
                return "redirect:/pago/" + citaId + "/exito";
                // FB PASOS 10-14 - CU-04 FINAL
            } catch (ReglasDeNegocioException exception) {
                // FA03 - CU-04 INICIO
                resultado.reject("pago.rechazado", exception.getMessage());
                pago.setCvv(null);
                model.addAttribute("cita", cita);
                model.addAttribute("claveIdempotencia", claveIdempotencia);
                return "pago/pago";
                // FA03 - CU-04 FINAL
            }


        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la cita solicitada.");
            return "redirect:/paciente/dashboard";
        }
    }
    // FB PASOS 7-14 - CU-04 FINAL


    // FA02 - CU-04 INICIO
    private String verificarCitaPagable(CitaDTO cita) {
        if (AppointmentStatus.PAGADA.name().equals(cita.getEstado())) {
            return "redirect:/pago/" + cita.getId() + "/exito";
        }
        if (!cita.isReservaSinVencimiento() && cita.getSegundosRestantes() == 0) {
            return "redirect:/citas/reintentar/" + cita.getId();
        }
        return null;
    }
    // FA02 - CU-04 FINAL

    // FB PASOS 14-15 - CU-04 INICIO
    @GetMapping("/pago/{citaId}/exito")
    public String pagoExitoso(@PathVariable Long citaId, Model model, RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = citaService.obtenerPorId(citaId);
            if (!AppointmentStatus.PAGADA.name().equals(cita.getEstado())) return "redirect:/pago/" + citaId;
            model.addAttribute("cita", cita);
            return "pago/exito";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se encontro la cita");
            return "redirect:/paciente/dashboard";
        }


    }
    // FB PASOS 14-15 - CU-04 FINAL



}
