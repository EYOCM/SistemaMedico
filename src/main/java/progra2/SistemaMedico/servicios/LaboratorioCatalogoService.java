package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.LaboratorioFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Laboratorio;
import progra2.SistemaMedico.repositorio.ExamenRepositorio;
import progra2.SistemaMedico.repositorio.LaboratorioRepositorio;
import progra2.SistemaMedico.util.CatalogoUtil;
import progra2.SistemaMedico.util.Paginacion;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LaboratorioCatalogoService {

    private final LaboratorioRepositorio repositorio;
    private final ExamenRepositorio examenRepositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 2 - CU-14 INICIO
    @Transactional(readOnly = true)
    public Page<Laboratorio> listar(String texto, int pagina, int tam) {
        return Paginacion.buscar(pagina, tam,
                pageable -> repositorio.findByNombreContainingIgnoreCase(CatalogoUtil.filtro(texto), pageable));
    }
    // FB PASO 2 - CU-14 FINAL

    @Transactional(readOnly = true)
    public List<Laboratorio> activos() {
        return repositorio.findByActivoTrueOrderByNombreAsc();
    }

    // FA02 - CU-14 INICIO
    @Transactional(readOnly = true)
    public LaboratorioFormDTO formulario(Long id) {
        Laboratorio l = buscar(id);
        return LaboratorioFormDTO.builder()
                .id(l.getId())
                .nombre(l.getNombre())
                .descripcion(l.getDescripcion())
                .activo(l.getActivo())
                .build();
    }
    // FA02 - CU-14 FINAL

    // FB PASOS 7-8 / FA02 - CU-14 INICIO
    @Transactional
    public String guardar(LaboratorioFormDTO form) {
        String nombre = form.getNombre().trim();
        // FA05 - CU-14 INICIO
        CatalogoUtil.validarNombreUnico(repositorio.findAll(), nombre, form.getId());
        // FA05 - CU-14 FINAL

        boolean nuevo = form.getId() == null;
        Laboratorio l = nuevo ? new Laboratorio() : buscar(form.getId());
        if (!nuevo && l.getActivo() && !form.getActivo()) {
            validarSinExamenesActivos(l);
        }
        l.setNombre(nombre);
        l.setDescripcion(CatalogoUtil.texto(form.getDescripcion()));
        l.setActivo(form.getActivo());
        repositorio.saveAndFlush(l);

        auditoriaService.registrar(nuevo ? "CATALOGO_CREADO" : "CATALOGO_ACTUALIZADO", "Laboratorio", l.getId(), nombre);
        return nuevo ? CatalogoUtil.creado(nombre) : CatalogoUtil.actualizado(nombre);
    }
    // FB PASOS 7-8 / FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    @Transactional
    public String eliminar(Long id) {
        Laboratorio l = buscar(id);
        validarSinExamenesActivos(l);
        l.setActivo(false);
        repositorio.saveAndFlush(l);
        auditoriaService.registrar("CATALOGO_ELIMINADO", "Laboratorio", l.getId(), l.getNombre());
        return CatalogoUtil.eliminado(l.getNombre());
    }
    // FA03 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    private void validarSinExamenesActivos(Laboratorio l) {
        long examenes = examenRepositorio.countByLaboratorioIdAndActivoTrue(l.getId());
        if (examenes > 0) {
            throw new ReglasDeNegocioException("No se puede eliminar el laboratorio " + l.getNombre()
                    + ": tiene " + examenes + " examen(es) activo(s). Asígnelos a otro laboratorio primero.");
        }
    }
    // FA03 - CU-14 FINAL

    private Laboratorio buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el laboratorio."));
    }
}
