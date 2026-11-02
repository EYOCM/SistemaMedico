package progra2.SistemaMedico.repositorio;


import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Cie10;

import java.util.List;

public interface Cie10Repositorio extends JpaRepository<Cie10, String> {

    List<Cie10> findTop10ByCodigoContainingIgnoreCaseOrDescripcionContainingIgnoreCaseOrderByCodigoAsc(
            String codigo, String descripcion);
}