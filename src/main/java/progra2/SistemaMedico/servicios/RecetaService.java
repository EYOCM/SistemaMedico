package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.RecetaFilaDTO;
import progra2.SistemaMedico.dto.RecetaFormDTO;
import progra2.SistemaMedico.dto.RecetaResumenDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Medicamento;
import progra2.SistemaMedico.model.Receta;
import progra2.SistemaMedico.model.RecetaDetalle;
import progra2.SistemaMedico.model.enums.EstadoReceta;
import progra2.SistemaMedico.repositorio.MedicamentoRepositorio;
import progra2.SistemaMedico.repositorio.RecetaRepositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecetaService {

    private static final String MSG_RN_CU08_03 =
            "Todos los campos de la receta son obligatorios excepto indicaciones especiales.";

    private final RecetaRepositorio recetaRepositorio;
    private final MedicamentoRepositorio medicamentoRepositorio;
    private final MedicoService medicoService;
    private final CitaMapper citaMapper;
    private final AuditoriaService auditoriaService;

    // FA04 - CU-08 INICIO
    @Transactional(readOnly = true)
    public CitaDTO citaEvaluada(Long citaId, String username) {
        return citaMapper.convertirADTO(medicoService.buscarEvaluada(citaId, username));
    }

    @Transactional(readOnly = true)
    public List<Medicamento> medicamentosActivos() {
        return medicamentoRepositorio.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<RecetaResumenDTO> buscarResumen(Long citaId) {
        return recetaRepositorio.findByCitaId(citaId).map(this::convertir);
    }

    @Transactional
    public RecetaResumenDTO generar(Long citaId, RecetaFormDTO form, String username) {
        Cita cita = medicoService.buscarEvaluada(citaId, username);
        if (recetaRepositorio.existsByCitaId(citaId)) {
            throw new ReglasDeNegocioException("Esta cita ya tiene una receta generada.");
        }

        List<RecetaFilaDTO> filas = form.getFilas().stream()
                .filter(Objects::nonNull)
                .filter(f -> !filaVacia(f))
                .toList();

        if (filas.isEmpty()) {
            throw new ReglasDeNegocioException("Agregue al menos un medicamento a la receta.");
        }

        Set<Long> usados = new HashSet<>();
        for (RecetaFilaDTO f : filas) {
            if (f.getMedicamentoId() == null || vacio(f.getDosis())
                    || vacio(f.getFrecuencia()) || vacio(f.getDuracion())) {
                throw new ReglasDeNegocioException(MSG_RN_CU08_03);
            }
            if (!usados.add(f.getMedicamentoId())) {
                throw new ReglasDeNegocioException("No repita el mismo medicamento en la receta.");
            }
        }

        Receta receta = Receta.builder()
                .numeroReceta(generarNumero())
                .cita(cita)
                .estado(EstadoReceta.PENDIENTE)
                .fechaEmision(LocalDateTime.now())
                .build();

        for (RecetaFilaDTO f : filas) {
            Medicamento medicamento = medicamentoRepositorio.findById(f.getMedicamentoId())
                    .filter(Medicamento::getActivo)
                    .orElseThrow(() -> new ReglasDeNegocioException(
                            "El medicamento seleccionado no existe en el catálogo."));

            receta.agregarDetalle(RecetaDetalle.builder()
                    .medicamento(medicamento)
                    .dosis(f.getDosis().trim())
                    .frecuencia(f.getFrecuencia().trim())
                    .duracion(f.getDuracion().trim())
                    .indicaciones(vacio(f.getIndicaciones()) ? null : f.getIndicaciones().trim())
                    .build());
        }

        recetaRepositorio.saveAndFlush(receta);
        auditoriaService.registrar("RECETA_GENERADA", "Cita", cita.getId(),
                "Receta " + receta.getNumeroReceta() + " con " + receta.getDetalles().size() + " medicamento(s)");

        return convertir(receta);
    }
    // FA04 - CU-08 FINAL

    private RecetaResumenDTO convertir(Receta receta) {
        List<RecetaFilaDTO> filas = receta.getDetalles().stream()
                .map(d -> RecetaFilaDTO.builder()
                        .medicamentoId(d.getMedicamento().getId())
                        .nombreMedicamento(d.getMedicamento().getNombre())
                        .dosis(d.getDosis())
                        .frecuencia(d.getFrecuencia())
                        .duracion(d.getDuracion())
                        .indicaciones(d.getIndicaciones())
                        .build())
                .toList();

        return RecetaResumenDTO.builder()
                .numeroReceta(receta.getNumeroReceta())
                .fechaEmision(receta.getFechaEmision())
                .estado(receta.getEstado().getDescripcion())
                .filas(filas)
                .build();
    }

    private boolean filaVacia(RecetaFilaDTO f) {
        return f.getMedicamentoId() == null && vacio(f.getDosis()) && vacio(f.getFrecuencia())
                && vacio(f.getDuracion()) && vacio(f.getIndicaciones());
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private String generarNumero() {
        String fecha = LocalDate.now().toString().replace("-", "");
        int aleatorio = (int) (Math.random() * 9000) + 1000;
        return "REC-" + fecha + "-" + aleatorio;
    }
}
