package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.SucursalFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Sucursal;
import progra2.SistemaMedico.repositorio.SucursalRepositorio;
import progra2.SistemaMedico.util.CatalogoUtil;
import progra2.SistemaMedico.util.Paginacion;

@Service
@RequiredArgsConstructor
public class SucursalCatalogoService {

    private final SucursalRepositorio repositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 2 - CU-14 INICIO
    @Transactional(readOnly = true)
    public Page<Sucursal> listar(String texto, int pagina, int tam) {
        return Paginacion.buscar(pagina, tam,
                pageable -> repositorio.findByNombreContainingIgnoreCase(CatalogoUtil.filtro(texto), pageable));
    }
    // FB PASO 2 - CU-14 FINAL

    // FA02 - CU-14 INICIO
    @Transactional(readOnly = true)
    public SucursalFormDTO formulario(Long id) {
        Sucursal s = buscar(id);
        return SucursalFormDTO.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .telefono(s.getTelefono())
                .direccion(s.getDireccion())
                .descripcion(s.getDescripcion())
                .email(s.getEmail())
                .activo(s.getActivo())
                .build();
    }
    // FA02 - CU-14 FINAL

    // FB PASOS 7-8 / FA02 - CU-14 INICIO
    @Transactional
    @CacheEvict(cacheNames = {"catalogoSucursales", "especialidadesPorSucursal"}, allEntries = true)
    public String guardar(SucursalFormDTO form) {
        String nombre = form.getNombre().trim();
        // FA05 - CU-14 INICIO
        CatalogoUtil.validarNombreUnico(repositorio.findAll(), nombre, form.getId());
        // FA05 - CU-14 FINAL

        boolean nuevo = form.getId() == null;
        Sucursal s = nuevo ? new Sucursal() : buscar(form.getId());
        s.setNombre(nombre);
        s.setTelefono(CatalogoUtil.texto(form.getTelefono()));
        s.setDireccion(CatalogoUtil.texto(form.getDireccion()));
        s.setDescripcion(CatalogoUtil.texto(form.getDescripcion()));
        s.setEmail(CatalogoUtil.texto(form.getEmail()));
        s.setActivo(form.getActivo());
        repositorio.saveAndFlush(s);

        auditoriaService.registrar(nuevo ? "CATALOGO_CREADO" : "CATALOGO_ACTUALIZADO", "Sucursal", s.getId(), nombre);
        return nuevo ? CatalogoUtil.creado(nombre) : CatalogoUtil.actualizado(nombre);
    }
    // FB PASOS 7-8 / FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    @Transactional
    @CacheEvict(cacheNames = {"catalogoSucursales", "especialidadesPorSucursal"}, allEntries = true)
    public String eliminar(Long id) {
        Sucursal s = buscar(id);
        s.setActivo(false);
        repositorio.saveAndFlush(s);
        auditoriaService.registrar("CATALOGO_ELIMINADO", "Sucursal", s.getId(), s.getNombre());
        return CatalogoUtil.eliminado(s.getNombre());
    }
    // FA03 - CU-14 FINAL

    private Sucursal buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la sucursal."));
    }
}
