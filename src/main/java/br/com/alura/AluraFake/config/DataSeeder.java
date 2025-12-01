package br.com.alura.AluraFake.config;

import br.com.alura.AluraFake.infra.entity.User;
import br.com.alura.AluraFake.infra.enumerated.Role;
import br.com.alura.AluraFake.infra.repository.CourseRepository;
import br.com.alura.AluraFake.infra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Não executar em ambiente de teste
        if ("test".equals(activeProfile)) return;

        if (userRepository.count() == 0) {
            // Senhas criptografadas com BCrypt
            String encodedPassword = passwordEncoder.encode("password123");

            User caio = new User("Caio", "caio@alura.com.br", Role.STUDENT, encodedPassword);
            User paulo = new User("Paulo", "paulo@alura.com.br", Role.INSTRUCTOR, encodedPassword);
            userRepository.saveAll(Arrays.asList(caio, paulo));
        }
    }
}