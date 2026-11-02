package progra2.SistemaMedico.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

public final class Paginacion {

    private static final List<Integer> TAMANOS = List.of(10, 25, 50);

    private Paginacion() {
    }

    public static int tamano(int tam) {
        return TAMANOS.contains(tam) ? tam : 10;
    }

    public static <T> Page<T> buscar(int pagina, int tam, Function<Pageable, Page<T>> consulta) {
        return buscar(pagina, tam, Sort.by("id"), consulta);
    }

    public static <T> Page<T> buscar(int pagina, int tam, Sort orden, Function<Pageable, Page<T>> consulta) {
        int tamano = tamano(tam);
        Page<T> resultado = consulta.apply(PageRequest.of(Math.max(pagina, 1) - 1, tamano, orden));
        if (resultado.getNumber() >= resultado.getTotalPages() && resultado.getTotalPages() > 0) {
            resultado = consulta.apply(PageRequest.of(resultado.getTotalPages() - 1, tamano, orden));
        }
        return resultado;
    }
}
