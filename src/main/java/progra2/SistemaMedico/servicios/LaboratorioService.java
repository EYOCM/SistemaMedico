package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.ExamenResultadoDTO;
import progra2.SistemaMedico.dto.OrdenLabDetalleDTO;
import progra2.SistemaMedico.dto.OrdenLabFilaDTO;
import progra2.SistemaMedico.dto.ResultadoExamenFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.OrdenExamen;
import progra2.SistemaMedico.model.OrdenLaboratorio;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.EstadoOrdenLaboratorio;
import progra2.SistemaMedico.repositorio.OrdenExamenRepositorio;
import progra2.SistemaMedico.repositorio.OrdenLaboratorioRepositorio;
import progra2.SistemaMedico.util.RangoReferencia;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaboratorioService {

    public static final String MENSAJE_RN_CU09_01 =
            "El pago debe estar completado antes de proceder con la toma de muestras.";

    private final OrdenLaboratorioRepositorio ordenRepositorio;
    private final OrdenExamenRepositorio ordenExamenRepositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 1 - CU-09 INICIO
    @Transactional(readOnly = true)
    public List<OrdenLabFilaDTO> listar(String estado, String paciente, String medico) {
        return ordenRepositorio.findAllByOrderByFechaCreacionDesc().stream()
                .filter(o -> vacio(estado) || o.getEstado().name().equals(estado))
                .filter(o -> vacio(paciente)
                        || contiene(nombreCompleto(o.getCita().getPaciente()), paciente)
                        || contiene(o.getCita().getPaciente().getDpi(), paciente))
                .filter(o -> vacio(medico) || contiene(nombreCompleto(o.getCita().getMedico()), medico))
                .map(this::aFila)
                .toList();
    }
    // FB PASO 1 - CU-09 FINAL

    // FB PASOS 2-3 - CU-09 INICIO
    @Transactional(readOnly = true)
    public OrdenLabDetalleDTO detalle(Long ordenId) {
        OrdenLaboratorio orden = ordenRepositorio.findById(ordenId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la orden de laboratorio."));

        List<ExamenResultadoDTO> examenes = orden.getExamenes().stream()
                .sorted(Comparator.comparing(OrdenExamen::getId))
                .map(this::aExamen)
                .toList();

        return OrdenLabDetalleDTO.builder()
                .id(orden.getId())
                .numeroOrden(orden.getNumeroOrden())
                .nombrePaciente(nombreCompleto(orden.getCita().getPaciente()))
                .dpi(orden.getCita().getPaciente().getDpi())
                .nombreMedico(nombreCompleto(orden.getCita().getMedico()))
                .estado(orden.getEstado().name())
                .estadoDescripcion(orden.getEstado().getDescripcion())
                .externa(orden.getExterna())
                .observaciones(orden.getObservaciones())
                .total(orden.getTotal())
                .fechaCreacion(orden.getFechaCreacion())
                .examenes(examenes)
                .build();
    }
    // FB PASOS 2-3 - CU-09 FINAL

    // FB PASOS 9-10 - CU-09 INICIO
    @Transactional
    public boolean guardarResultado(Long ordenId, Long examenId, ResultadoExamenFormDTO datos) {
        OrdenExamen examen = buscarEditable(ordenId, examenId);

        examen.setValorResultado(datos.getValorResultado().trim());
        examen.setUnidadResultado(vacio(datos.getUnidadResultado())
                ? examen.getExamen().getUnidad()
                : datos.getUnidadResultado().trim());
        examen.setFechaResultado(datos.getFechaResultado());
        // FA02 - CU-09 INICIO
        examen.setFueraDeRango(datos.isFueraDeRango());
        // FA02 - CU-09 FINAL
        examen.setNotasResultado(vacio(datos.getNotasResultado()) ? null : datos.getNotasResultado().trim());
        ordenExamenRepositorio.saveAndFlush(examen);

        auditoriaService.registrar("RESULTADO_GUARDADO", "OrdenLaboratorio", ordenId,
                examen.getExamen().getNombre() + " = " + examen.getValorResultado()
                        + (examen.getFueraDeRango() ? " (fuera de rango)" : ""));

        return examen.getFueraDeRango();
    }
    // FB PASOS 9-10 - CU-09 FINAL

    // FB PASOS 11-14 - CU-09 INICIO
    @Transactional
    public boolean publicar(Long ordenId, Long examenId) {
        OrdenExamen examen = buscarEditable(ordenId, examenId);
        if (examen.getValorResultado() == null) {
            throw new ReglasDeNegocioException("Guarde el resultado antes de publicarlo.");
        }

        examen.setPublicado(true);
        examen.setFechaPublicacion(LocalDateTime.now());
        ordenExamenRepositorio.saveAndFlush(examen);
        auditoriaService.registrar("RESULTADO_PUBLICADO", "OrdenLaboratorio", ordenId,
                examen.getExamen().getNombre());

        // FB PASO 14 - CU-09 INICIO
        OrdenLaboratorio orden = examen.getOrden();
        boolean todosPublicados = orden.getExamenes().stream().allMatch(OrdenExamen::getPublicado);
        if (todosPublicados) {
            orden.setEstado(EstadoOrdenLaboratorio.COMPLETADA);
            ordenRepositorio.saveAndFlush(orden);
            auditoriaService.registrar("ORDEN_COMPLETADA", "OrdenLaboratorio", ordenId,
                    "Orden " + orden.getNumeroOrden() + " completada");
        }
        // FB PASO 14 - CU-09 FINAL
        return todosPublicados;
    }
    // FB PASOS 11-14 - CU-09 FINAL

    // FB PASO 7 - CU-09 INICIO
    private OrdenExamen buscarEditable(Long ordenId, Long examenId) {
        OrdenExamen examen = ordenExamenRepositorio.findById(examenId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el examen."));
        OrdenLaboratorio orden = examen.getOrden();

        if (!orden.getId().equals(ordenId)) {
            throw new ReglasDeNegocioException("El examen no pertenece a esta orden.");
        }
        // FA01 - CU-09 INICIO
        if (orden.getExterna()) {
            throw new ReglasDeNegocioException(
                    "Esta orden es externa: los resultados los presenta el paciente a su médico.");
        }
        // FA01 - CU-09 FINAL
        // FB PASO 6 - CU-09 INICIO
        if (orden.getEstado() == EstadoOrdenLaboratorio.PENDIENTE) {
            throw new ReglasDeNegocioException(MENSAJE_RN_CU09_01);
        }
        // FB PASO 6 - CU-09 FINAL
        if (examen.getPublicado()) {
            throw new ReglasDeNegocioException("Este resultado ya fue publicado y no puede modificarse.");
        }
        return examen;
    }
    // FB PASO 7 - CU-09 FINAL

    private OrdenLabFilaDTO aFila(OrdenLaboratorio orden) {
        return OrdenLabFilaDTO.builder()
                .id(orden.getId())
                .numeroOrden(orden.getNumeroOrden())
                .nombrePaciente(nombreCompleto(orden.getCita().getPaciente()))
                .dpi(orden.getCita().getPaciente().getDpi())
                .nombreMedico(nombreCompleto(orden.getCita().getMedico()))
                .fechaCreacion(orden.getFechaCreacion())
                .estado(orden.getEstado().name())
                .estadoDescripcion(orden.getEstado().getDescripcion())
                .externa(orden.getExterna())
                .cantidadExamenes(orden.getExamenes().size())
                .publicados(orden.getExamenes().stream().filter(OrdenExamen::getPublicado).count())
                .build();
    }

    private ExamenResultadoDTO aExamen(OrdenExamen oe) {
        return ExamenResultadoDTO.builder()
                .id(oe.getId())
                .nombre(oe.getExamen().getNombre())
                .precio(oe.getPrecio())
                .rangoReferencia(oe.getExamen().getRangoReferencia())
                .unidadCatalogo(oe.getExamen().getUnidad())
                .valorResultado(oe.getValorResultado())
                .unidadResultado(oe.getUnidadResultado())
                .fechaResultado(oe.getFechaResultado())
                .fueraDeRango(oe.getFueraDeRango())
                .notasResultado(oe.getNotasResultado())
                .publicado(oe.getPublicado())
                .fechaPublicacion(oe.getFechaPublicacion())
                // FA02 - CU-09 INICIO
                .sugerenciaFueraDeRango(RangoReferencia
                        .estaFuera(oe.getValorResultado(), oe.getExamen().getRangoReferencia())
                        .orElse(null))
                // FA02 - CU-09 FINAL
                .build();
    }

    private String nombreCompleto(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellidos();
    }

    private boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase().contains(buscado.trim().toLowerCase());
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
