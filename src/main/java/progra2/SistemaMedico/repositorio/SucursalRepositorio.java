package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Sucursal;

import java.util.List;

public interface SucursalRepositorio extends JpaRepository<Sucursal, Long> {
    //metodo para obtener sucursales activas
List<Sucursal> findByActivoTrue();

    Page<Sucursal> findByNombreContainingIgnoreCase(String texto, Pageable pageable);
}
