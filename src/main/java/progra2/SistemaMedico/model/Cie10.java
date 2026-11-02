package progra2.SistemaMedico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cie10")
public class Cie10 {

    @Id
    @Column(length = 10)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String descripcion;
}
