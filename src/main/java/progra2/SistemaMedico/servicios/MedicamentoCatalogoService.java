package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.MedicamentoFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Medicamento;
import progra2.SistemaMedico.repositorio.MedicamentoRepositorio;
import progra2.SistemaMedico.util.CatalogoUtil;
import progra2.SistemaMedico.util.Paginacion;

@Service
@RequiredArgsConstructor
public class MedicamentoCatalogoService {

    private final MedicamentoRepositorio repositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 2 - CU-14 INICIO
    @Transactional(readOnly = true)
    public Page<Medicamento> listar(String texto, int pagina, int tam) {
        return Paginacion.buscar(pagina, tam,
                pageable -> repositorio.findByNombreContainingIgnoreCase(CatalogoUtil.filtro(texto), pageable));
    }
    // FB PASO 2 - CU-14 FINAL

    // FA02 - CU-14 INICIO
    @Transactional(readOnly = true)
    public MedicamentoFormDTO formulario(Long id) {
        Medicamento m = buscar(id);
        return MedicamentoFormDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .precio(m.getPrecio())
                .unidad(m.getUnidad())
                .controlado(m.getControlado())
                .stockMinimo(m.getStockMinimo())
                .activo(m.getActivo())
                .build();
    }
    // FA02 - CU-14 FINAL

    // FB PASOS 7-8 / FA02 - CU-14 INICIO
    @Transactional
    public String guardar(MedicamentoFormDTO form) {
        String nombre = form.getNombre().trim();
        // FA05 - CU-14 INICIO
        CatalogoUtil.validarNombreUnico(repositorio.findAll(), nombre, form.getId());
        // FA05 - CU-14 FINAL

        boolean nuevo = form.getId() == null;
        Medicamento m = nuevo ? new Medicamento() : buscar(form.getId());
        m.setNombre(nombre);
        m.setDescripcion(form.getDescripcion().trim());
        m.setPrecio(form.getPrecio());
        m.setUnidad(form.getUnidad().trim());
        m.setControlado(form.isControlado());
        m.setStockMinimo(form.getStockMinimo() == null ? 0 : form.getStockMinimo());
        m.setActivo(form.getActivo());
        repositorio.saveAndFlush(m);

        auditoriaService.registrar(nuevo ? "CATALOGO_CREADO" : "CATALOGO_ACTUALIZADO", "Medicamento", m.getId(), nombre);
        return nuevo ? CatalogoUtil.creado(nombre) : CatalogoUtil.actualizado(nombre);
    }
    // FB PASOS 7-8 / FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    @Transactional
    public String eliminar(Long id) {
        Medicamento m = buscar(id);
        m.setActivo(false);
        repositorio.saveAndFlush(m);
        auditoriaService.registrar("CATALOGO_ELIMINADO", "Medicamento", m.getId(), m.getNombre());
        return CatalogoUtil.eliminado(m.getNombre());
    }
    // FA03 - CU-14 FINAL

    private Medicamento buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el medicamento."));
    }
}
