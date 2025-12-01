package br.com.alura.AluraFake.infra;

import br.com.alura.AluraFake.course.*;
import br.com.alura.AluraFake.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, CourseRepository courseRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Não executar em ambiente de teste ou quando não for dev
        if ("test".equals(activeProfile) || !"dev".equals(activeProfile)) return;

        if (userRepository.count() == 0) {
            // Senhas criptografadas com BCrypt
            String encodedPassword = passwordEncoder.encode("password123");

            User caio = new User("Caio", "caio@alura.com.br", Role.STUDENT, encodedPassword);
            User paulo = new User("Paulo", "paulo@alura.com.br", Role.INSTRUCTOR, encodedPassword);
            userRepository.saveAll(Arrays.asList(caio, paulo));
            courseRepository.save(new Course("Java", "Aprenda Java com Alura", paulo));
        }
    }
}