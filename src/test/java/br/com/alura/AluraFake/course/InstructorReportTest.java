package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.infra.entity.Course;
import br.com.alura.AluraFake.infra.enumerated.Status;
import br.com.alura.AluraFake.infra.repository.CourseRepository;
import br.com.alura.AluraFake.infra.entity.Task;
import br.com.alura.AluraFake.infra.repository.TaskRepository;
import br.com.alura.AluraFake.infra.enumerated.Type;
import br.com.alura.AluraFake.infra.enumerated.Role;
import br.com.alura.AluraFake.infra.entity.User;
import br.com.alura.AluraFake.infra.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "INSTRUCTOR")
public class InstructorReportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User instructor;
    private User student;

    @BeforeEach
    public void setup() {
        taskRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        instructor = new User("Instrutor", "instrutor@test.com", Role.INSTRUCTOR);
        instructor = userRepository.save(instructor);

        student = new User("Estudante", "estudante@test.com", Role.STUDENT);
        student = userRepository.save(student);
    }

    // ======================== TESTES DE RELATÓRIO ========================

    @Test
    public void shouldReturnInstructorCoursesSuccessfully() throws Exception {
        // Cria 2 cursos para o instrutor
        Course course1 = new Course("Java Basics", "Curso de Java", instructor);
        Course course2 = new Course("Spring Boot", "Curso de Spring", instructor);
        course1 = courseRepository.save(course1);
        course2 = courseRepository.save(course2);

        // Adiciona atividades ao curso1
        Task task1 = new Task("O que é Java?", 1, Type.OPEN_TEXT, course1);
        Task task2 = new Task("O que significa JVM?", 2, Type.SINGLE_CHOICE, course1);
        taskRepository.save(task1);
        taskRepository.save(task2);

        // Adiciona 1 atividade ao curso2
        Task task3 = new Task("O que é Spring?", 1, Type.OPEN_TEXT, course2);
        taskRepository.save(task3);

        // Publica curso1
        course1.setStatus(Status.PUBLISHED);
        courseRepository.save(course1);

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").isArray())
                .andExpect(jsonPath("$.courses.length()").value(2))
                .andExpect(jsonPath("$.totalPublishedCourses").value(1));
    }

    @Test
    public void shouldReturnEmptyListWhenInstructorHasNoCourses() throws Exception {
        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").isArray())
                .andExpect(jsonPath("$.courses.length()").value(0))
                .andExpect(jsonPath("$.totalPublishedCourses").value(0));
    }

    @Test
    public void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/instructor/999999/courses"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn400WhenUserIsNotInstructor() throws Exception {
        mockMvc.perform(get("/instructor/" + student.getId() + "/courses"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnCorrectTaskCount() throws Exception {
        Course course = new Course("Java Basics", "Curso de Java", instructor);
        course = courseRepository.save(course);

        // Adiciona 5 atividades
        for (int i = 1; i <= 5; i++) {
            Task task = new Task("Atividade " + i, i, Type.OPEN_TEXT, course);
            taskRepository.save(task);
        }

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].taskCount").value(5));
    }

    @Test
    public void shouldReturnPublishedAtWhenCourseIsPublished() throws Exception {
        Course course = new Course("Java Basics", "Curso de Java", instructor);
        course.setStatus(Status.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        course = courseRepository.save(course);

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].publishedAt").exists());
    }

    @Test
    public void shouldNotReturnPublishedAtWhenCourseIsBuilding() throws Exception {
        Course course = new Course("Java Basics", "Curso de Java", instructor);
        course = courseRepository.save(course);

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].publishedAt").doesNotExist());
    }

    @Test
    public void shouldReturnAllCourseFields() throws Exception {
        Course course = new Course("Java Basics", "Curso de Java", instructor);
        course = courseRepository.save(course);

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].id").value(course.getId()))
                .andExpect(jsonPath("$.courses[0].title").value("Java Basics"))
                .andExpect(jsonPath("$.courses[0].status").value("BUILDING"))
                .andExpect(jsonPath("$.courses[0].taskCount").value(0));
    }

    @Test
    public void shouldCountOnlyPublishedCourses() throws Exception {
        // Cria 3 cursos
        Course course1 = new Course("Java Basics", "Curso de Java", instructor);
        Course course2 = new Course("Spring Boot", "Curso de Spring", instructor);
        Course course3 = new Course("React", "Curso de React", instructor);

        course1 = courseRepository.save(course1);
        course2 = courseRepository.save(course2);
        course3 = courseRepository.save(course3);

        // Publica apenas 2 cursos
        course1.setStatus(Status.PUBLISHED);
        course2.setStatus(Status.PUBLISHED);
        courseRepository.save(course1);
        courseRepository.save(course2);

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses.length()").value(3))
                .andExpect(jsonPath("$.totalPublishedCourses").value(2));
    }

    @Test
    public void shouldNotReturnCoursesFromOtherInstructors() throws Exception {
        User otherInstructor = new User("Outro Instrutor", "outro@test.com", Role.INSTRUCTOR);
        otherInstructor = userRepository.save(otherInstructor);

        // Cria curso para o instrutor principal
        Course course1 = new Course("Java Basics", "Curso de Java", instructor);
        courseRepository.save(course1);

        // Cria curso para outro instrutor
        Course course2 = new Course("Python Basics", "Curso de Python", otherInstructor);
        courseRepository.save(course2);

        mockMvc.perform(get("/instructor/" + instructor.getId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses.length()").value(1))
                .andExpect(jsonPath("$.courses[0].title").value("Java Basics"));
    }
}

