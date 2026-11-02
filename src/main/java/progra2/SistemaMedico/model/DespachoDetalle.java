package progra2.SistemaMedico.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "despacho_detalles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despacho_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Despacho despacho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_detalle_id", nullable = false)
    private RecetaDetalle recetaDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private Boolean sustituido;

    @Column(length = 500)
    private String razonSustitucion;
}
