package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.task.Task;
import br.com.alura.AluraFake.task.TaskRepository;
import br.com.alura.AluraFake.task.Type;
import br.com.alura.AluraFake.user.Role;
import br.com.alura.AluraFake.user.User;
import br.com.alura.AluraFake.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CoursePublishTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private Course course;
    private User instructor;

    @BeforeEach
    public void setup() {
        taskRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        instructor = new User("Instrutor", "instrutor@test.com", Role.INSTRUCTOR);
        instructor = userRepository.save(instructor);

        course = new Course("Java Basics", "Curso de Java", instructor);
        course = courseRepository.save(course);
    }

    // ======================== TESTES DE PUBLICAÇÃO ========================

    @Test
    public void shouldPublishCourseSuccessfully() throws Exception {
        // Cria uma atividade de cada tipo
        Task openText = new Task("O que é Java?", 1, Type.OPEN_TEXT, course);
        Task singleChoice = new Task("O que significa JVM?", 2, Type.SINGLE_CHOICE, course);
        Task multipleChoice = new Task("Quais são conceitos de POO?", 3, Type.MULTIPLE_CHOICE, course);

        taskRepository.save(openText);
        taskRepository.save(singleChoice);
        taskRepository.save(multipleChoice);

        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isOk());

        // Verifica se o curso foi publicado
        Course publishedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals(Status.PUBLISHED, publishedCourse.getStatus());
        assertNotNull(publishedCourse.getPublishedAt());
    }

    @Test
    public void shouldRejectPublishWithoutOpenTextTask() throws Exception {
        Task singleChoice = new Task("O que significa JVM?", 1, Type.SINGLE_CHOICE, course);
        Task multipleChoice = new Task("Quais são conceitos de POO?", 2, Type.MULTIPLE_CHOICE, course);

        taskRepository.save(singleChoice);
        taskRepository.save(multipleChoice);

        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectPublishWithoutSingleChoiceTask() throws Exception {
        Task openText = new Task("O que é Java?", 1, Type.OPEN_TEXT, course);
        Task multipleChoice = new Task("Quais são conceitos de POO?", 2, Type.MULTIPLE_CHOICE, course);

        taskRepository.save(openText);
        taskRepository.save(multipleChoice);

        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectPublishWithoutMultipleChoiceTask() throws Exception {
        Task openText = new Task("O que é Java?", 1, Type.OPEN_TEXT, course);
        Task singleChoice = new Task("O que significa JVM?", 2, Type.SINGLE_CHOICE, course);

        taskRepository.save(openText);
        taskRepository.save(singleChoice);

        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectPublishWithoutTasks() throws Exception {
        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectPublishWhenAlreadyPublished() throws Exception {
        // Cria atividades
        Task openText = new Task("O que é Java?", 1, Type.OPEN_TEXT, course);
        Task singleChoice = new Task("O que significa JVM?", 2, Type.SINGLE_CHOICE, course);
        Task multipleChoice = new Task("Quais são conceitos de POO?", 3, Type.MULTIPLE_CHOICE, course);

        taskRepository.save(openText);
        taskRepository.save(singleChoice);
        taskRepository.save(multipleChoice);

        // Publica pela primeira vez
        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isOk());

        // Tenta publicar novamente
        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectPublishWithNonSequentialOrders() throws Exception {
        // Cria atividades com ordem não sequencial (1, 2, 4)
        Task openText = new Task("O que é Java?", 1, Type.OPEN_TEXT, course);
        Task singleChoice = new Task("O que significa JVM?", 2, Type.SINGLE_CHOICE, course);
        Task multipleChoice = new Task("Quais são conceitos de POO?", 4, Type.MULTIPLE_CHOICE, course);

        taskRepository.save(openText);
        taskRepository.save(singleChoice);
        taskRepository.save(multipleChoice);

        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectPublishNonExistentCourse() throws Exception {
        mockMvc.perform(post("/course/999999/publish"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldPublishCourseWithManyTasks() throws Exception {
        // Cria múltiplas atividades de cada tipo
        Task openText1 = new Task("O que é Java?", 1, Type.OPEN_TEXT, course);
        Task singleChoice1 = new Task("O que significa JVM?", 2, Type.SINGLE_CHOICE, course);
        Task multipleChoice1 = new Task("Quais são conceitos de POO?", 3, Type.MULTIPLE_CHOICE, course);
        Task openText2 = new Task("O que é JDK?", 4, Type.OPEN_TEXT, course);
        Task singleChoice2 = new Task("O que é compilador?", 5, Type.SINGLE_CHOICE, course);

        taskRepository.save(openText1);
        taskRepository.save(singleChoice1);
        taskRepository.save(multipleChoice1);
        taskRepository.save(openText2);
        taskRepository.save(singleChoice2);

        mockMvc.perform(post("/course/" + course.getId() + "/publish"))
                .andExpect(status().isOk());

        Course publishedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals(Status.PUBLISHED, publishedCourse.getStatus());
    }
}

