package br.com.alura.AluraFake.service;

import br.com.alura.AluraFake.infra.entity.Course;
import br.com.alura.AluraFake.infra.entity.Task;
import br.com.alura.AluraFake.infra.repository.CourseRepository;
import br.com.alura.AluraFake.infra.enumerated.Status;
import br.com.alura.AluraFake.infra.dto.course.InstructorCourseDTO;
import br.com.alura.AluraFake.infra.dto.course.InstructorCoursesReportDTO;
import br.com.alura.AluraFake.infra.repository.TaskRepository;
import br.com.alura.AluraFake.infra.enumerated.Type;
import br.com.alura.AluraFake.infra.entity.User;
import br.com.alura.AluraFake.infra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    @Autowired
    @Lazy
    private CourseRepository courseRepository;
    @Autowired
    @Lazy
    private TaskRepository taskRepository;
    @Autowired
    @Lazy
    private UserRepository userRepository;

    @Transactional
    public void publishCourse(Long courseId) {
        Course course = findCourseOrThrow(courseId);
        List<Task> tasks = taskRepository.findByCourseIdOrderByOrderNumberAsc(courseId);

        validateCourseCanBePublished(course, tasks);
        updateCourseToPublished(course);
    }

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Curso não encontrado"));
    }

    private void validateCourseCanBePublished(Course course, List<Task> tasks) {
        ensureCourseIsInBuildingStatus(course);
        ensureCourseHasTasks(tasks);
        ensureAllRequiredTaskTypesExist(tasks);
        ensureTasksAreInSequentialOrder(tasks);
    }

    private void ensureCourseIsInBuildingStatus(Course course) {
        if (course.getStatus() != Status.BUILDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso só pode ser publicado se o status for BUILDING");
        }
    }

    private void ensureCourseHasTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso deve conter ao menos uma atividade para ser publicado");
        }
    }

    private void ensureAllRequiredTaskTypesExist(List<Task> tasks) {
        Map<Type, Long> tasksByType = tasks.stream()
                .collect(Collectors.groupingBy(Task::getType, Collectors.counting()));

        Map<Type, String> requiredTypes = Map.of(
                Type.OPEN_TEXT, "resposta aberta (OPEN_TEXT)",
                Type.SINGLE_CHOICE, "alternativa única (SINGLE_CHOICE)",
                Type.MULTIPLE_CHOICE, "múltipla escolha (MULTIPLE_CHOICE)"
        );

        requiredTypes.forEach((type, description) -> {
            if (!tasksByType.containsKey(type)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O curso deve conter ao menos uma atividade de " + description);
            }
        });
    }

    private void ensureTasksAreInSequentialOrder(List<Task> tasks) {
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
    }

    private void updateCourseToPublished(Course course) {
        course.setStatus(Status.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    public InstructorCoursesReportDTO generateInstructorReport(Long instructorId) {
        User instructor = findInstructorOrThrow(instructorId);
        List<Course> courses = courseRepository.findByInstructorId(instructorId);
        List<InstructorCourseDTO> courseDTOs = buildCourseDTOs(courses);
        long totalPublished = countPublishedCourses(instructorId);

        return new InstructorCoursesReportDTO(courseDTOs, totalPublished);
    }

    private User findInstructorOrThrow(Long instructorId) {
        User user = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"));

        ensureUserIsInstructor(user);
        return user;
    }

    private void ensureUserIsInstructor(User user) {
        if (!user.isInstructor()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário não é um instrutor");
        }
    }

    private List<InstructorCourseDTO> buildCourseDTOs(List<Course> courses) {
        return courses.stream()
                .map(this::buildCourseDTO)
                .collect(Collectors.toList());
    }

    private InstructorCourseDTO buildCourseDTO(Course course) {
        long taskCount = taskRepository.countByCourseId(course.getId());
        return new InstructorCourseDTO(course, taskCount);
    }

    private long countPublishedCourses(Long instructorId) {
        return courseRepository.countByInstructorIdAndStatus(instructorId, Status.PUBLISHED);
    }
}

