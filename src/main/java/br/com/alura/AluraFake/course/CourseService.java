package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.course.dto.InstructorCourseDTO;
import br.com.alura.AluraFake.course.dto.InstructorCoursesReportDTO;
import br.com.alura.AluraFake.task.TaskRepository;
import br.com.alura.AluraFake.task.Type;
import br.com.alura.AluraFake.user.User;
import br.com.alura.AluraFake.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void publishCourse(Long courseId) {
        // Verifica se o curso existe
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Curso não encontrado"));

        // Verifica se o curso está em BUILDING
        if (course.getStatus() != Status.BUILDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso só pode ser publicado se o status for BUILDING");
        }

        // Busca todas as atividades do curso
        var tasks = taskRepository.findByCourseIdOrderByOrderNumberAsc(courseId);

        if (tasks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso deve conter ao menos uma atividade para ser publicado");
        }

        // Verifica se contém ao menos uma atividade de cada tipo
        Map<Type, Long> tasksByType = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getType(), Collectors.counting()));

        if (!tasksByType.containsKey(Type.OPEN_TEXT)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso deve conter ao menos uma atividade de resposta aberta (OPEN_TEXT)");
        }

        if (!tasksByType.containsKey(Type.SINGLE_CHOICE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso deve conter ao menos uma atividade de alternativa única (SINGLE_CHOICE)");
        }

        if (!tasksByType.containsKey(Type.MULTIPLE_CHOICE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso deve conter ao menos uma atividade de múltipla escolha (MULTIPLE_CHOICE)");
        }

        // Verifica se a ordem está em sequência contínua (1, 2, 3...)
        for (int i = 0; i < tasks.size(); i++) {
            int expectedOrder = i + 1;
            int actualOrder = tasks.get(i).getOrderNumber();

            if (actualOrder != expectedOrder) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("As atividades devem ter ordem em sequência contínua. " +
                                "Esperado ordem %d, mas encontrado ordem %d", expectedOrder, actualOrder));
            }
        }

        // Atualiza o status para PUBLISHED e define publishedAt
        course.setStatus(Status.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    public InstructorCoursesReportDTO generateInstructorReport(Long instructorId) {
        // Verifica se o usuário existe
        User user = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"));

        // Verifica se o usuário é instrutor
        if (!user.isInstructor()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário não é um instrutor");
        }

        // Busca todos os cursos do instrutor
        List<Course> courses = courseRepository.findByInstructorId(instructorId);

        // Cria lista de DTOs com quantidade de atividades
        List<InstructorCourseDTO> courseDTOs = courses.stream()
                .map(course -> {
                    long taskCount = taskRepository.countByCourseId(course.getId());
                    return new InstructorCourseDTO(course, taskCount);
                })
                .collect(Collectors.toList());

        // Conta total de cursos publicados
        long totalPublished = courseRepository.countByInstructorIdAndStatus(instructorId, Status.PUBLISHED);

        return new InstructorCoursesReportDTO(courseDTOs, totalPublished);
    }
}

