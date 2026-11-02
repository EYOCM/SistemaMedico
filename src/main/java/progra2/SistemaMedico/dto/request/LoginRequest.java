package progra2.SistemaMedico.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    // en esta clase se recibe la informacion desde postman o el ciente  para loguearse
    @NotBlank(message = "Este campo es obligatorio")
    private String username;

    @NotBlank(message = "Este campo es obligatorio")
    private String password;

}
