package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.CourseRepository;
import br.com.alura.AluraFake.task.dto.request.TaskRequestDTO;
import br.com.alura.AluraFake.user.Role;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "INSTRUCTOR")
public class TaskOrderValidationTest {

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

        User instructor = new User("Instrutor", "instrutor@test.com", Role.INSTRUCTOR);
        instructor = userRepository.save(instructor);

        course = new Course("Java Basics", "Curso de Java", instructor);
        course = courseRepository.save(course);
    }

    // ======================== TESTES DE VALIDAÇÃO DE SEQUÊNCIA ========================

    @Test
    public void shouldAcceptFirstTaskWithOrder1() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), "Primeira atividade", 1);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldRejectFirstTaskWithOrderDifferentThan1() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO(course.getId(), "Primeira atividade", 2);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRejectTaskWithOrderJump() throws Exception {
        // Cria atividade ordem 1
        TaskRequestDTO dto1 = new TaskRequestDTO(course.getId(), "Atividade 1", 1);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        // Cria atividade ordem 2
        TaskRequestDTO dto2 = new TaskRequestDTO(course.getId(), "Atividade 2", 2);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        // Tenta criar atividade ordem 4 (salto - deve falhar)
        TaskRequestDTO dto4 = new TaskRequestDTO(course.getId(), "Atividade 4", 4);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto4)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptSequentialOrders() throws Exception {
        // Ordem 1
        TaskRequestDTO dto1 = new TaskRequestDTO(course.getId(), "Atividade 1", 1);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        // Ordem 2
        TaskRequestDTO dto2 = new TaskRequestDTO(course.getId(), "Atividade 2", 2);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        // Ordem 3
        TaskRequestDTO dto3 = new TaskRequestDTO(course.getId(), "Atividade 3", 3);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto3)))
                .andExpect(status().isCreated());
    }

    // ======================== TESTES DE DESLOCAMENTO DE ORDEM ========================

    @Test
    public void shouldShiftTasksWhenInsertingInMiddle() throws Exception {
        // Cria atividades em ordem: 1, 2, 3
        TaskRequestDTO dto1 = new TaskRequestDTO(course.getId(), "Atividade A", 1);
        TaskRequestDTO dto2 = new TaskRequestDTO(course.getId(), "Atividade B", 2);
        TaskRequestDTO dto3 = new TaskRequestDTO(course.getId(), "Atividade C", 3);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto3)))
                .andExpect(status().isCreated());

        // Insere nova atividade na ordem 2 (deve deslocar B e C)
        TaskRequestDTO dtoNew = new TaskRequestDTO(course.getId(), "Nova Atividade", 2);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoNew)))
                .andExpect(status().isCreated());

        // Verifica que as ordens foram atualizadas corretamente
        List<Task> tasks = taskRepository.findAll();
        assertEquals(4, tasks.size());

        // Ordem esperada: 1-A, 2-Nova, 3-B, 4-C
        Task taskOrder1 = tasks.stream().filter(t -> t.getOrderNumber() == 1).findFirst().orElseThrow();
        assertEquals("Atividade A", taskOrder1.getStatement());

        Task taskOrder2 = tasks.stream().filter(t -> t.getOrderNumber() == 2).findFirst().orElseThrow();
        assertEquals("Nova Atividade", taskOrder2.getStatement());

        Task taskOrder3 = tasks.stream().filter(t -> t.getOrderNumber() == 3).findFirst().orElseThrow();
        assertEquals("Atividade B", taskOrder3.getStatement());

        Task taskOrder4 = tasks.stream().filter(t -> t.getOrderNumber() == 4).findFirst().orElseThrow();
        assertEquals("Atividade C", taskOrder4.getStatement());
    }

    @Test
    public void shouldShiftTasksWhenInsertingAtBeginning() throws Exception {
        // Cria atividades em ordem: 1, 2
        TaskRequestDTO dto1 = new TaskRequestDTO(course.getId(), "Atividade A", 1);
        TaskRequestDTO dto2 = new TaskRequestDTO(course.getId(), "Atividade B", 2);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        // Insere nova atividade na ordem 1 (deve deslocar A e B)
        TaskRequestDTO dtoNew = new TaskRequestDTO(course.getId(), "Nova Primeira", 1);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoNew)))
                .andExpect(status().isCreated());

        // Verifica que as ordens foram atualizadas corretamente
        List<Task> tasks = taskRepository.findAll();
        assertEquals(3, tasks.size());

        Task taskOrder1 = tasks.stream().filter(t -> t.getOrderNumber() == 1).findFirst().orElseThrow();
        assertEquals("Nova Primeira", taskOrder1.getStatement());

        Task taskOrder2 = tasks.stream().filter(t -> t.getOrderNumber() == 2).findFirst().orElseThrow();
        assertEquals("Atividade A", taskOrder2.getStatement());

        Task taskOrder3 = tasks.stream().filter(t -> t.getOrderNumber() == 3).findFirst().orElseThrow();
        assertEquals("Atividade B", taskOrder3.getStatement());
    }

    @Test
    public void shouldNotShiftWhenInsertingAtEnd() throws Exception {
        // Cria atividades em ordem: 1, 2
        TaskRequestDTO dto1 = new TaskRequestDTO(course.getId(), "Atividade A", 1);
        TaskRequestDTO dto2 = new TaskRequestDTO(course.getId(), "Atividade B", 2);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        // Insere nova atividade na ordem 3 (não deve deslocar ninguém)
        TaskRequestDTO dtoNew = new TaskRequestDTO(course.getId(), "Atividade C", 3);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoNew)))
                .andExpect(status().isCreated());

        // Verifica que as ordens estão corretas
        List<Task> tasks = taskRepository.findAll();
        assertEquals(3, tasks.size());

        Task taskOrder1 = tasks.stream().filter(t -> t.getOrderNumber() == 1).findFirst().orElseThrow();
        assertEquals("Atividade A", taskOrder1.getStatement());

        Task taskOrder2 = tasks.stream().filter(t -> t.getOrderNumber() == 2).findFirst().orElseThrow();
        assertEquals("Atividade B", taskOrder2.getStatement());

        Task taskOrder3 = tasks.stream().filter(t -> t.getOrderNumber() == 3).findFirst().orElseThrow();
        assertEquals("Atividade C", taskOrder3.getStatement());
    }

    @Test
    public void shouldHandleComplexInsertionScenario() throws Exception {
        // Cenário: 1-A, 2-B, 3-C
        // Inserir em 2: 1-A, 2-Nova, 3-B, 4-C
        // Inserir em 1: 1-Outra, 2-A, 3-Nova, 4-B, 5-C

        TaskRequestDTO dtoA = new TaskRequestDTO(course.getId(), "Atividade A", 1);
        TaskRequestDTO dtoB = new TaskRequestDTO(course.getId(), "Atividade B", 2);
        TaskRequestDTO dtoC = new TaskRequestDTO(course.getId(), "Atividade C", 3);

        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoA)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoB)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoC)))
                .andExpect(status().isCreated());

        // Inserir em 2
        TaskRequestDTO dtoNova = new TaskRequestDTO(course.getId(), "Nova Atividade", 2);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoNova)))
                .andExpect(status().isCreated());

        // Inserir em 1
        TaskRequestDTO dtoOutra = new TaskRequestDTO(course.getId(), "Outra Atividade", 1);
        mockMvc.perform(post("/task/new/opentext")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoOutra)))
                .andExpect(status().isCreated());

        // Verificar ordem final: 1-Outra, 2-A, 3-Nova, 4-B, 5-C
        List<Task> tasks = taskRepository.findAll();
        assertEquals(5, tasks.size());

        Task task1 = tasks.stream().filter(t -> t.getOrderNumber() == 1).findFirst().orElseThrow();
        assertEquals("Outra Atividade", task1.getStatement());

        Task task2 = tasks.stream().filter(t -> t.getOrderNumber() == 2).findFirst().orElseThrow();
        assertEquals("Atividade A", task2.getStatement());

        Task task3 = tasks.stream().filter(t -> t.getOrderNumber() == 3).findFirst().orElseThrow();
        assertEquals("Nova Atividade", task3.getStatement());

        Task task4 = tasks.stream().filter(t -> t.getOrderNumber() == 4).findFirst().orElseThrow();
        assertEquals("Atividade B", task4.getStatement());

        Task task5 = tasks.stream().filter(t -> t.getOrderNumber() == 5).findFirst().orElseThrow();
        assertEquals("Atividade C", task5.getStatement());
    }
}

