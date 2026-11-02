package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventario_medicamentos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"medicamento_id", "sucursal_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventarioMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private Boolean activo;

    @Version
    private Long version;
}
