package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.OrdenExamenLineaDTO;
import progra2.SistemaMedico.dto.OrdenLaboratorioFormDTO;
import progra2.SistemaMedico.dto.OrdenLaboratorioResumenDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Examen;
import progra2.SistemaMedico.model.OrdenExamen;
import progra2.SistemaMedico.model.OrdenLaboratorio;
import progra2.SistemaMedico.model.enums.EstadoOrdenLaboratorio;
import progra2.SistemaMedico.repositorio.ExamenRepositorio;
import progra2.SistemaMedico.repositorio.OrdenLaboratorioRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenLaboratorioService {

    private final OrdenLaboratorioRepositorio ordenRepositorio;
    private final ExamenRepositorio examenRepositorio;
    private final MedicoService medicoService;
    private final CitaMapper citaMapper;
    private final AuditoriaService auditoriaService;

    // FA01 - CU-08 INICIO
    @Transactional(readOnly = true)
    public CitaDTO citaEvaluada(Long citaId, String username) {
        return citaMapper.convertirADTO(medicoService.buscarEvaluada(citaId, username));
    }

    @Transactional(readOnly = true)
    public List<Examen> examenesActivos() {
        return examenRepositorio.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<OrdenLaboratorioResumenDTO> buscarResumen(Long citaId) {
        return ordenRepositorio.findByCitaId(citaId).map(this::convertir);
    }

    @Transactional
    public OrdenLaboratorioResumenDTO generar(Long citaId, OrdenLaboratorioFormDTO form, String username) {
        Cita cita = medicoService.buscarEvaluada(citaId, username);
        if (ordenRepositorio.existsByCitaId(citaId)) {
            throw new ReglasDeNegocioException("Esta cita ya tiene una orden de laboratorio.");
        }

        List<Long> ids = form.getExamenIds() == null ? List.of() : form.getExamenIds().stream().distinct().toList();
        if (ids.isEmpty()) {
            throw new ReglasDeNegocioException("Seleccione al menos un examen.");
        }

        List<Examen> examenes = examenRepositorio.findAllById(ids);
        if (examenes.size() != ids.size() || examenes.stream().anyMatch(e -> !e.getActivo())) {
            throw new ReglasDeNegocioException("Uno de los exámenes seleccionados no existe en el catálogo.");
        }

        BigDecimal total = examenes.stream()
                .map(Examen::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrdenLaboratorio orden = OrdenLaboratorio.builder()
                .numeroOrden(generarNumero())
                .cita(cita)
                .estado(EstadoOrdenLaboratorio.PENDIENTE)
                // FA01 - CU-09 INICIO
                .externa(form.isExterna())
                // FA01 - CU-09 FINAL
                .observaciones(form.getObservaciones() == null || form.getObservaciones().isBlank()
                        ? null : form.getObservaciones().trim())
                .total(total)
                .fechaCreacion(LocalDateTime.now())
                .build();

        for (Examen examen : examenes) {
            orden.agregarExamen(OrdenExamen.builder()
                    .examen(examen)
                    .precio(examen.getPrecio())
                    .build());
        }

        ordenRepositorio.saveAndFlush(orden);
        auditoriaService.registrar("ORDEN_LAB_GENERADA", "Cita", cita.getId(),
                "Orden " + orden.getNumeroOrden() + " con " + examenes.size() + " examen(es)"
                        + (orden.getExterna() ? " (externa)" : ""));

        return convertir(orden);
    }
    // FA01 - CU-08 FINAL

    private OrdenLaboratorioResumenDTO convertir(OrdenLaboratorio orden) {
        List<OrdenExamenLineaDTO> lineas = orden.getExamenes().stream()
                .map(oe -> OrdenExamenLineaDTO.builder()
                        .nombre(oe.getExamen().getNombre())
                        .precio(oe.getPrecio())
                        .build())
                .toList();

        return OrdenLaboratorioResumenDTO.builder()
                .numeroOrden(orden.getNumeroOrden())
                .estado(orden.getEstado().getDescripcion())
                .externa(orden.getExterna())
                .observaciones(orden.getObservaciones())
                .total(orden.getTotal())
                .fechaCreacion(orden.getFechaCreacion())
                .examenes(lineas)
                .build();
    }

    private String generarNumero() {
        String fecha = LocalDate.now().toString().replace("-", "");
        int aleatorio = (int) (Math.random() * 9000) + 1000;
        return "LAB-" + fecha + "-" + aleatorio;
    }
}
