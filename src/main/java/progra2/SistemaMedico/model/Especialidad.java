package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "especialidad")
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Especialidad {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Column(length = 500)
    private String descripcion;
@Column(nullable = false)
    private Boolean activo = true;

@CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;

@UpdateTimestamp
    private LocalDateTime fechaActualizacion;

}
