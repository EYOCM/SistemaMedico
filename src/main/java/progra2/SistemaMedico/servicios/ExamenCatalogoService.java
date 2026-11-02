package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.ExamenFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Examen;
import progra2.SistemaMedico.model.Laboratorio;
import progra2.SistemaMedico.repositorio.ExamenRepositorio;
import progra2.SistemaMedico.repositorio.LaboratorioRepositorio;
import progra2.SistemaMedico.util.CatalogoUtil;
import progra2.SistemaMedico.util.Paginacion;

@Service
@RequiredArgsConstructor
public class ExamenCatalogoService {

    private final ExamenRepositorio repositorio;
    private final LaboratorioRepositorio laboratorioRepositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 2 - CU-14 INICIO
    @Transactional(readOnly = true)
    public Page<Examen> listar(String texto, int pagina, int tam) {
        return Paginacion.buscar(pagina, tam,
                pageable -> repositorio.findByNombreContainingIgnoreCase(CatalogoUtil.filtro(texto), pageable));
    }
    // FB PASO 2 - CU-14 FINAL

    // FA02 - CU-14 INICIO
    @Transactional(readOnly = true)
    public ExamenFormDTO formulario(Long id) {
        Examen e = buscar(id);
        return ExamenFormDTO.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .precio(e.getPrecio())
                .rangoReferencia(e.getRangoReferencia())
                .unidad(e.getUnidad())
                .laboratorioId(e.getLaboratorio() == null ? null : e.getLaboratorio().getId())
                .activo(e.getActivo())
                .build();
    }
    // FA02 - CU-14 FINAL

    // FB PASOS 7-8 / FA02 - CU-14 INICIO
    @Transactional
    public String guardar(ExamenFormDTO form) {
        String nombre = form.getNombre().trim();
        // FA05 - CU-14 INICIO
        CatalogoUtil.validarNombreUnico(repositorio.findAll(), nombre, form.getId());
        // FA05 - CU-14 FINAL
        Laboratorio laboratorio = laboratorioRepositorio.findById(form.getLaboratorioId())
                .filter(Laboratorio::getActivo)
                .orElseThrow(() -> new ReglasDeNegocioException("Debe seleccionar un laboratorio."));

        boolean nuevo = form.getId() == null;
        Examen e = nuevo ? new Examen() : buscar(form.getId());
        e.setNombre(nombre);
        e.setDescripcion(CatalogoUtil.texto(form.getDescripcion()));
        e.setPrecio(form.getPrecio());
        e.setRangoReferencia(CatalogoUtil.texto(form.getRangoReferencia()));
        e.setUnidad(CatalogoUtil.texto(form.getUnidad()));
        e.setLaboratorio(laboratorio);
        e.setActivo(form.getActivo());
        repositorio.saveAndFlush(e);

        auditoriaService.registrar(nuevo ? "CATALOGO_CREADO" : "CATALOGO_ACTUALIZADO", "Examen", e.getId(), nombre);
        return nuevo ? CatalogoUtil.creado(nombre) : CatalogoUtil.actualizado(nombre);
    }
    // FB PASOS 7-8 / FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    @Transactional
    public String eliminar(Long id) {
        Examen e = buscar(id);
        e.setActivo(false);
        repositorio.saveAndFlush(e);
        auditoriaService.registrar("CATALOGO_ELIMINADO", "Examen", e.getId(), e.getNombre());
        return CatalogoUtil.eliminado(e.getNombre());
    }
    // FA03 - CU-14 FINAL

    private Examen buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el examen."));
    }
}
