package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Receta;
import progra2.SistemaMedico.model.enums.EstadoReceta;

import java.util.Optional;

public interface RecetaRepositorio extends JpaRepository<Receta, Long> {

    Optional<Receta> findByCitaId(Long citaId);

    boolean existsByCitaId(Long citaId);

    Optional<Receta> findByNumeroRecetaAndEstado(String numeroReceta, EstadoReceta estado);

    Optional<Receta> findByCitaNumeroCitaAndEstado(String numeroCita, EstadoReceta estado);
}
