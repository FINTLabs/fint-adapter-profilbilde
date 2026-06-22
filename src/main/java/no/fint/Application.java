package no.fint;

import no.fint.sse.oauth.OAuthConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@EnableScheduling
@Import(OAuthConfig.class)
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(Application.class, args);
    }

}
