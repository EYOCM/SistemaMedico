package progra2.SistemaMedico.validacion;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LuhnValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

public @interface Luhn {
    String message() default "El número de tarjeta no es válido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
