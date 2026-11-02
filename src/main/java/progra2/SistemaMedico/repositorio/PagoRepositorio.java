package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Pago;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {
}
