package progra2.SistemaMedico.util;

import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Catalogo;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class CatalogoUtil {

    private CatalogoUtil() {
    }

    // FA05 - CU-14 INICIO
    public static void validarNombreUnico(List<? extends Catalogo> registros, String nombre, Long idActual) {
        String buscado = comparable(nombre);
        registros.stream()
                .filter(c -> !c.getId().equals(idActual) && comparable(c.getNombre()).equals(buscado))
                .findFirst()
                .ifPresent(c -> {
                    if (c.getActivo()) {
                        throw new ReglasDeNegocioException("Ya existe un registro con el nombre " + c.getNombre() + ".");
                    }
                    throw new ReglasDeNegocioException("Ya existe un registro inactivo con el nombre " + c.getNombre()
                            + ". Edítelo y márquelo como Activo.");
                });
    }
    // FA05 - CU-14 FINAL

    private static String comparable(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public static String filtro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    // FB PASO 8 - CU-14 INICIO
    public static String creado(String nombre) {
        return "El registro " + nombre + " ha sido creado exitosamente.";
    }
    // FB PASO 8 - CU-14 FINAL

    // FA02 - CU-14 INICIO
    public static String actualizado(String nombre) {
        return "El registro " + nombre + " ha sido actualizado correctamente.";
    }
    // FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    public static String eliminado(String nombre) {
        return "El registro " + nombre + " ha sido eliminado correctamente.";
    }
    // FA03 - CU-14 FINAL
}
