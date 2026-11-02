package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CREAR, ACTUALIZAR o ELIMINAR */
    @Column(nullable = false, length = 20, updatable = false)
    private String accion;

    /** Nombre de la entidad afectada, por ejemplo "Usuario" */
    @Column(nullable = false, length = 50, updatable = false)
    private String entidad;

    /** Id del registro afectado */
    @Column(updatable = false)
    private Long entidadId;

    /** Username de quien ejecuto la accion */
    @Column(nullable = false, length = 50, updatable = false)
    private String ejecutadoPor;

    /** Descripcion legible de lo que ocurrio */
    @Column(length = 500, updatable = false)
    private String detalle;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHora;
}
