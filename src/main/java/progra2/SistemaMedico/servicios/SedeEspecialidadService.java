package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.SedeEspecialidadFilaDTO;
import progra2.SistemaMedico.dto.SedeEspecialidadFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Especialidad;
import progra2.SistemaMedico.model.Sucursal;
import progra2.SistemaMedico.model.SucursalEspecialidad;
import progra2.SistemaMedico.repositorio.EspecialidadRepositorio;
import progra2.SistemaMedico.repositorio.SucursalEspecialidadRepositorio;
import progra2.SistemaMedico.repositorio.SucursalRepositorio;
import progra2.SistemaMedico.util.Paginacion;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SedeEspecialidadService {

    private static final String MENSAJE_DUPLICADO = "Esta combinación de sede y especialidad ya existe en el sistema.";

    private final SucursalEspecialidadRepositorio repositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final EspecialidadRepositorio especialidadRepositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 3 - CU-13 INICIO
    @Transactional(readOnly = true)
    public Page<SedeEspecialidadFilaDTO> listar(Long id, int pagina, int tam) {
        return Paginacion.buscar(pagina, tam, pageable -> buscar(id, pageable)).map(this::aFila);
    }
    // FB PASO 3 - CU-13 FINAL

    // FB PASOS 8-9 - CU-13 INICIO
    @Transactional
    @CacheEvict(value = "especialidadesPorSucursal", allEntries = true)
    public String asignar(SedeEspecialidadFormDTO form) {
        Sucursal sucursal = sucursalActiva(form.getSucursalId());
        Especialidad especialidad = especialidadActiva(form.getEspecialidadId());

        // FA05 - CU-13 INICIO
        Optional<SucursalEspecialidad> existente =
                repositorio.findBySucursalIdAndEspecialidadId(sucursal.getId(), especialidad.getId());
        if (existente.filter(SucursalEspecialidad::getActivo).isPresent()) {
            throw new ReglasDeNegocioException(MENSAJE_DUPLICADO);
        }
        // FA05 - CU-13 FINAL

        SucursalEspecialidad asignacion = existente.orElseGet(() -> SucursalEspecialidad.builder()
                .sucursal(sucursal)
                .especialidad(especialidad)
                .build());
        asignacion.setActivo(true);
        // FA05 - CU-13 INICIO
        try {
            repositorio.saveAndFlush(asignacion);
        } catch (DataIntegrityViolationException e) {
            throw new ReglasDeNegocioException(MENSAJE_DUPLICADO);
        }
        // FA05 - CU-13 FINAL

        // FB PASO 9 - CU-13 INICIO
        auditoriaService.registrar(existente.isPresent() ? "SEDE_ESP_REACTIVADA" : "SEDE_ESP_ASIGNADA",
                "SucursalEspecialidad", asignacion.getId(), sucursal.getNombre() + " · " + especialidad.getNombre());
        return "Especialidad asignada a la sede correctamente";
        // FB PASO 9 - CU-13 FINAL
    }
    // FB PASOS 8-9 - CU-13 FINAL

    // FA02 - CU-13 INICIO
    @Transactional
    @CacheEvict(value = "especialidadesPorSucursal", allEntries = true)
    public String eliminar(Long id) {
        SucursalEspecialidad asignacion = repositorio.findById(id)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la asignación."));
        if (!asignacion.getActivo()) {
            throw new ReglasDeNegocioException("Esta asignación ya estaba eliminada.");
        }
        asignacion.setActivo(false);
        repositorio.saveAndFlush(asignacion);

        String detalle = asignacion.getSucursal().getNombre() + " · " + asignacion.getEspecialidad().getNombre();
        auditoriaService.registrar("SEDE_ESP_ELIMINADA", "SucursalEspecialidad", asignacion.getId(), detalle);
        return "Asignación " + detalle + " eliminada correctamente.";
    }
    // FA02 - CU-13 FINAL

    // FB PASO 3 - CU-13 INICIO
    private Page<SucursalEspecialidad> buscar(Long id, Pageable pageable) {
        return id == null ? repositorio.findAll(pageable) : repositorio.findByIdEquals(id, pageable);
    }
    // FB PASO 3 - CU-13 FINAL

    private Sucursal sucursalActiva(Long id) {
        // FA03 - CU-13 INICIO
        if (id == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una sede.");
        }
        // FA03 - CU-13 FINAL
        return sucursalRepositorio.findById(id)
                .filter(Sucursal::getActivo)
                .orElseThrow(() -> new ReglasDeNegocioException("La sede seleccionada no existe o está inactiva."));
    }

    private Especialidad especialidadActiva(Long id) {
        // FA03 - CU-13 INICIO
        if (id == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una especialidad.");
        }
        // FA03 - CU-13 FINAL
        return especialidadRepositorio.findById(id)
                .filter(Especialidad::getActivo)
                .orElseThrow(() -> new ReglasDeNegocioException("La especialidad seleccionada no existe o está inactiva."));
    }

    private SedeEspecialidadFilaDTO aFila(SucursalEspecialidad se) {
        return SedeEspecialidadFilaDTO.builder()
                .id(se.getId())
                .nombreSucursal(se.getSucursal().getNombre())
                .nombreEspecialidad(se.getEspecialidad().getNombre())
                .activo(se.getActivo())
                .build();
    }
}
