package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import progra2.SistemaMedico.validacion.Luhn;

//DTO PARA RECIBIR LOS DATOS DEL FORMULARIO
@Data
public class PagoDTO {
    // FB PASOS 3-6 - CU-04 INICIO
    @NotBlank(message = "El número de tarjeta es obligatorio.")
    @Pattern(regexp = "^\\d*$", message = "El número de tarjeta debe contener solo dígitos.")
    @Size(min = 13, max = 19, message = "El número de tarjeta debe contener entre 13 y 19 dígitos y ser válido.")
    @Luhn(message = "El número de tarjeta no es válido.")
    private String numeroTarjeta;

    @NotBlank(message = "El nombre del titular es obligatorio.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]*$",
            message = "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.")
    @Size(min = 5, max = 100,
            message = "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.")
    private String nombreTitular;

    @NotBlank(message = "La fecha de vencimiento es obligatoria.")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Formato inválido. Use MM/AA")
    private String  vencimiento;

    @NotBlank(message = "El CVV es obligatorio.")
    @Pattern(regexp = "^\\d{3,4}$", message = "El CVV debe contener 3 ó 4 dígitos numéricos.")
    private String  cvv;
    // FB PASOS 3-6 - CU-04 FINAL


}
