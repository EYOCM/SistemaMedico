package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import progra2.SistemaMedico.dto.SucursalDTO;
import progra2.SistemaMedico.model.Especialidad;
import progra2.SistemaMedico.model.Sucursal;
import progra2.SistemaMedico.repositorio.EspecialidadRepositorio;
import progra2.SistemaMedico.repositorio.SucursalRepositorio;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final SucursalRepositorio sucursalRepositorio;
    private final EspecialidadRepositorio especialidadRepositorio;

    @Cacheable("catalogoSucursales")
    public List<Sucursal> sucursalesActivas() {
        System.out.println("Leyendo sucursales activas en base de datos");
        return sucursalRepositorio.findByActivoTrue();
    }

    @Cacheable("catalogoEspecialidades")
    public List<Especialidad> especiaidadesActivas (){
        System.out.println("Leyendo especialidades activas en base de datos");
        return especialidadRepositorio.findByActivoTrue();
    }

    @CacheEvict(value = "catalogoSucursales", allEntries = true)
    public void invalidarSucursales(){
        System.out.println("[CACHE] sucursales invalidas");
    }

    @CacheEvict(value = "catalogoEspecialidades", allEntries = true)
    public void invalidarEspecialidades(){
        System.out.println("[CAHE] especialidades invalidas");
    }
}
