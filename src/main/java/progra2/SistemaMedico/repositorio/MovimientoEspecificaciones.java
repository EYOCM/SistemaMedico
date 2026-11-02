package progra2.SistemaMedico.repositorio;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import progra2.SistemaMedico.dto.FiltroMovimientoDTO;
import progra2.SistemaMedico.model.MovimientoInventario;

import java.util.ArrayList;
import java.util.List;

public final class MovimientoEspecificaciones {

    private MovimientoEspecificaciones() {
    }

    // FB PASO 2 - CU-15 INICIO
    public static Specification<MovimientoInventario> filtrar(FiltroMovimientoDTO f, Long sucursalFija) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            Long sucursalId = sucursalFija != null ? sucursalFija : f.getSucursalId();
            if (sucursalId != null) {
                condiciones.add(cb.equal(raiz.get("sucursal").get("id"), sucursalId));
            }
            if (f.getTipo() != null) {
                condiciones.add(cb.equal(raiz.get("tipo"), f.getTipo()));
            }
            if (f.getMedicamentoId() != null) {
                condiciones.add(cb.equal(raiz.get("medicamento").get("id"), f.getMedicamentoId()));
            }
            if (tieneTexto(f.getReferencia())) {
                condiciones.add(cb.like(cb.lower(raiz.get("referencia")), "%" + f.getReferencia().trim().toLowerCase() + "%"));
            }
            if (tieneTexto(f.getUsuario())) {
                condiciones.add(cb.like(cb.lower(raiz.get("usuario")), "%" + f.getUsuario().trim().toLowerCase() + "%"));
            }
            if (f.getDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), f.getDesde().atStartOfDay()));
            }
            if (f.getHasta() != null) {
                condiciones.add(cb.lessThan(raiz.get("fecha"), f.getHasta().plusDays(1).atStartOfDay()));
            }
            if (!f.isInactivos()) {
                condiciones.add(cb.isTrue(raiz.get("activo")));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
    // FB PASO 2 - CU-15 FINAL

    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}
