package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.CourseRepository;
import br.com.alura.AluraFake.course.Status;
import br.com.alura.AluraFake.task.dto.request.OptionDTO;
import br.com.alura.AluraFake.task.dto.request.SingleChoiceTaskRequestDTO;
import br.com.alura.AluraFake.task.dto.request.TaskRequestDTO;
import br.com.alura.AluraFake.user.User;
import br.com.alura.AluraFake.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "INSTRUCTOR")
public class TaskControllerTest {

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

    private Course course;

    @BeforeEach
    public void setup() {
        taskRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário instrutor
        User instructor = new User("Instrutor", "instrutor@test.com", br.com.alura.AluraFake.user.Role.INSTRUCTOR);
        instructor = userRepository.save(instructor);

        // Criar curso em BUILDING
        course = new Course("Java Basics", "Curso de Java", instructor);
        course = courseRepository.save(course);
    }

    // ======================== TESTES DE OPEN TEXT ========================

    @Test
    public void shouldCreateOpenTextTaskSuccessfully() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), "Qual é a diferença entre JDK e JRE?", 1);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldRejectOpenTextTaskWithShortStatement() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), "Oi?", 1);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectOpenTextTaskWithLongStatement() throws Exception {
        String longStatement = "A".repeat(256);
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), longStatement, 1);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectOpenTextTaskWithDuplicateStatement() throws Exception {
        String statement = "Qual é a diferença entre JDK e JRE?";
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), statement, 1);

        // Criar primeira task
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Tentar criar segunda task com mesmo enunciado
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectOpenTextTaskWithNegativeOrder() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), "Qual é a diferença entre JDK e JRE?", -1);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectOpenTextTaskWhenCourseIsPublished() throws Exception {
        course.setStatus(Status.PUBLISHED);
        courseRepository.save(course);

        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), "Qual é a diferença entre JDK e JRE?", 1);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ======================== TESTES DE SINGLE CHOICE ========================

    @Test
    public void shouldCreateSingleChoiceTaskSuccessfully() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Java Virtual Machine", true),
                new OptionDTO("JavaScript Virtual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithNoCorrectOption() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Java Virtual Machine", false),
                new OptionDTO("JavaScript Virtual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithMultipleCorrectOptions() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Java Virtual Machine", true),
                new OptionDTO("JavaScript Virtual Machine", true)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithOnlyOneOption() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Java Virtual Machine", true)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithMoreThanFiveOptions() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Java Virtual Machine", true),
                new OptionDTO("JavaScript Virtual Machine", false),
                new OptionDTO("Java Version Manager", false),
                new OptionDTO("JavaScript Version Manager", false),
                new OptionDTO("Java Vendor Machine", false),
                new OptionDTO("Java Visual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithDuplicateOptions() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Java Virtual Machine", true),
                new OptionDTO("Java Virtual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithOptionEqualToStatement() throws Exception {
        String statement = "O que significa JVM?";
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO(statement, true),
                new OptionDTO("JavaScript Virtual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), statement, 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithShortOption() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("JVM", true),
                new OptionDTO("JavaScript Virtual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectSingleChoiceTaskWithLongOption() throws Exception {
        String longOption = "A".repeat(81);
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO(longOption, true),
                new OptionDTO("JavaScript Virtual Machine", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "O que significa JVM?", 1, options);

        mockMvc.perform(post("/task/new/singlechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ======================== TESTES DE MULTIPLE CHOICE ========================

    @Test
    public void shouldCreateMultipleChoiceTaskSuccessfully() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Herança", true),
                new OptionDTO("Polimorfismo", true),
                new OptionDTO("Variável global", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "Quais são conceitos de POO?", 1, options);

        mockMvc.perform(post("/task/new/multiplechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldRejectMultipleChoiceTaskWithOnlyOneCorrectOption() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Herança", true),
                new OptionDTO("Polimorfismo", false),
                new OptionDTO("Variável global", false)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "Quais são conceitos de POO?", 1, options);

        mockMvc.perform(post("/task/new/multiplechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectMultipleChoiceTaskWithAllCorrectOptions() throws Exception {
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO("Herança", true),
                new OptionDTO("Polimorfismo", true),
                new OptionDTO("Encapsulamento", true)
        );
        SingleChoiceTaskRequestDTO dto = new SingleChoiceTaskRequestDTO(
                course.getId(), "Quais são conceitos de POO?", 1, options);

        mockMvc.perform(post("/task/new/multiplechoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}

