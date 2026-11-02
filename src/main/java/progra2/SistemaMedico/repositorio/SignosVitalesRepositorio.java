package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.SignosVitales;

import java.util.List;

public interface SignosVitalesRepositorio extends JpaRepository<SignosVitales, Long> {


    List<SignosVitales> findByCitaPacienteIdOrderByFechaRegistroDesc(Long pacienteId);
}
