package progra2.SistemaMedico.tareas;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import progra2.SistemaMedico.servicios.SeguimientoService;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RecordatorioSeguimientoTarea {

    private final SeguimientoService seguimientoService;

    // FB PASO 10 - CU-12 INICIO
    @Scheduled(cron = "${app.recordatorio-seguimiento.cron:0 0 * * * *}")
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now().plusDays(1);
        int enviados = seguimientoService.enviarRecordatorios(manana);
        if (enviados > 0) {
            System.out.println("Recordatorios de seguimiento enviados para el " + manana + ": " + enviados);
        }
    }
    // FB PASO 10 - CU-12 FINAL
}
