package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.EspecialidadFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Especialidad;
import progra2.SistemaMedico.repositorio.EspecialidadRepositorio;
import progra2.SistemaMedico.util.CatalogoUtil;
import progra2.SistemaMedico.util.Paginacion;

@Service
@RequiredArgsConstructor
public class EspecialidadCatalogoService {

    private final EspecialidadRepositorio repositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 2 - CU-14 INICIO
    @Transactional(readOnly = true)
    public Page<Especialidad> listar(String texto, int pagina, int tam) {
        return Paginacion.buscar(pagina, tam,
                pageable -> repositorio.findByNombreContainingIgnoreCase(CatalogoUtil.filtro(texto), pageable));
    }
    // FB PASO 2 - CU-14 FINAL

    // FA02 - CU-14 INICIO
    @Transactional(readOnly = true)
    public EspecialidadFormDTO formulario(Long id) {
        Especialidad e = buscar(id);
        return EspecialidadFormDTO.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .precioConsulta(e.getPrecioConsulta())
                .activo(e.getActivo())
                .build();
    }
    // FA02 - CU-14 FINAL

    // FB PASOS 7-8 / FA02 - CU-14 INICIO
    @Transactional
    @CacheEvict(cacheNames = {"catalogoEspecialidades", "especialidadesPorSucursal"}, allEntries = true)
    public String guardar(EspecialidadFormDTO form) {
        String nombre = form.getNombre().trim();
        // FA05 - CU-14 INICIO
        CatalogoUtil.validarNombreUnico(repositorio.findAll(), nombre, form.getId());
        // FA05 - CU-14 FINAL

        boolean nuevo = form.getId() == null;
        Especialidad e = nuevo ? new Especialidad() : buscar(form.getId());
        e.setNombre(nombre);
        e.setDescripcion(form.getDescripcion().trim());
        e.setPrecioConsulta(form.getPrecioConsulta());
        e.setActivo(form.getActivo());
        repositorio.saveAndFlush(e);

        auditoriaService.registrar(nuevo ? "CATALOGO_CREADO" : "CATALOGO_ACTUALIZADO", "Especialidad", e.getId(), nombre);
        return nuevo ? CatalogoUtil.creado(nombre) : CatalogoUtil.actualizado(nombre);
    }
    // FB PASOS 7-8 / FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    @Transactional
    @CacheEvict(cacheNames = {"catalogoEspecialidades", "especialidadesPorSucursal"}, allEntries = true)
    public String eliminar(Long id) {
        Especialidad e = buscar(id);
        e.setActivo(false);
        repositorio.saveAndFlush(e);
        auditoriaService.registrar("CATALOGO_ELIMINADO", "Especialidad", e.getId(), e.getNombre());
        return CatalogoUtil.eliminado(e.getNombre());
    }
    // FA03 - CU-14 FINAL

    private Especialidad buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la especialidad."));
    }
}
