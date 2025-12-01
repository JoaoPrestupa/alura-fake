package br.com.alura.AluraFake.user;

import br.com.alura.AluraFake.infra.entity.User;
import br.com.alura.AluraFake.infra.enumerated.Role;
import br.com.alura.AluraFake.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail__should_return_existis_user() {
        User caio = new User("Joao", "joao@alura.com.br", Role.STUDENT);
        userRepository.save(caio);

        Optional<User> result = userRepository.findByEmail("joao@alura.com.br");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Joao");

        result = userRepository.findByEmail("laercio@alura.com.br");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail__should_return_true_when_user_existis() {
        User caio = new User("Joao", "joao@alura.com.br", Role.STUDENT);
        userRepository.save(caio);

        assertThat(userRepository.existsByEmail("joao@alura.com.br")).isTrue();
        assertThat(userRepository.existsByEmail("laercio@alura.com.br")).isFalse();
    }

}