package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Despacho;

public interface DespachoRepositorio extends JpaRepository<Despacho, Long> {
}
