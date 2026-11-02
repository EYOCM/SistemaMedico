package progra2.SistemaMedico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching   // habilita @Cacheable  (catalogos)
@EnableAsync     // habilita @Async      (correo de bienvenida)
@EnableScheduling
public class SistemaMedicoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaMedicoApplication.class, args);
	}

}
