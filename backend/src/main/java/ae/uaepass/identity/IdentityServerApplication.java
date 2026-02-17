package ae.uaepass.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IdentityServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServerApplication.class, args);
    }
}
