package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Consulta;

import java.util.Optional;

public interface ConsultaRepositorio  extends JpaRepository<Consulta, Long> {

    Optional<Consulta> findByCitaId(Long citaId);
}
