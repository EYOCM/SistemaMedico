package progra2.SistemaMedico.servicios.serviceImpl;

import progra2.SistemaMedico.dto.*;


import java.time.LocalDate;
import java.util.List;

public interface ICitaService {
    List<SucursalDTO> obtenerSucursalesActivas();
    List<EspecialidadDTO> obtenerEspecialidadesPorSucursal(Long sucursalId);
    List<UsuarioDTO> obtenerMedicosDisponibles(Long sucursalId, Long especialidadId);
    List<String> obtenerHorariosDisponibles(Long medicoId, LocalDate fecha);
    CitaDTO agendarCita(ApiResponse.SolicitudCitaDTO solicitudCitaDTO);
    List<CitaDTO> obtenerCitasPorPaciente(Long pacienteId);
    CitaDTO obtenerPorId(Long id);

}
