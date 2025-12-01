package br.com.alura.AluraFake.service;

import br.com.alura.AluraFake.infra.entity.Course;
import br.com.alura.AluraFake.infra.entity.Task;
import br.com.alura.AluraFake.infra.repository.CourseRepository;
import br.com.alura.AluraFake.infra.enumerated.Status;
import br.com.alura.AluraFake.infra.dto.task.OptionDTO;
import br.com.alura.AluraFake.infra.dto.task.SingleChoiceTaskRequestDTO;
import br.com.alura.AluraFake.infra.dto.task.TaskRequestDTO;
import br.com.alura.AluraFake.infra.repository.TaskRepository;
import br.com.alura.AluraFake.infra.enumerated.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Transactional
    public void createOpenTextTask(TaskRequestDTO dto) {
        Course course = validateAndGetCourse(dto.getCourseId(), dto.getStatement());
        validateAndHandleOrderSequence(dto.getCourseId(), dto.getOrder());

        Task task = new Task(dto.getStatement(), dto.getOrder(), Type.OPEN_TEXT, course);
        taskRepository.save(task);
    }

    @Transactional
    public void createSingleChoiceTask(SingleChoiceTaskRequestDTO dto) {
        Course course = validateAndGetCourse(dto.getCourseId(), dto.getStatement());
        validateSingleChoiceOptions(dto.getOptions(), dto.getStatement());
        validateAndHandleOrderSequence(dto.getCourseId(), dto.getOrder());

        Task task = new Task(dto.getStatement(), dto.getOrder(), Type.SINGLE_CHOICE, course);
        taskRepository.save(task);
    }

    @Transactional
    public void createMultipleChoiceTask(SingleChoiceTaskRequestDTO dto) {
        Course course = validateAndGetCourse(dto.getCourseId(), dto.getStatement());
        validateMultipleChoiceOptions(dto.getOptions(), dto.getStatement());
        validateAndHandleOrderSequence(dto.getCourseId(), dto.getOrder());

        Task task = new Task(dto.getStatement(), dto.getOrder(), Type.MULTIPLE_CHOICE, course);
        taskRepository.save(task);
    }

    private Course validateAndGetCourse(Long courseId, String statement) {
        // Verifica se o curso existe
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Curso não encontrado"));

        // Verifica se o curso está em BUILDING
        if (course.getStatus() != Status.BUILDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Um curso só pode receber atividades se seu status for BUILDING");
        }

        // Verifica se já existe uma questão com o mesmo enunciado no curso
        if (taskRepository.existsByCourseIdAndStatement(courseId, statement)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O curso não pode ter duas questões com o mesmo enunciado");
        }

        return course;
    }

    private void validateSingleChoiceOptions(List<OptionDTO> options, String statement) {
        int correctCount = validateCommonOptionsRules(options, statement, 2, 5);

        if (correctCount != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade deve ter uma única alternativa correta");
        }
    }

    private void validateMultipleChoiceOptions(List<OptionDTO> options, String statement) {
        int correctCount = validateCommonOptionsRules(options, statement, 2, 5);

        if (correctCount < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade de múltipla escolha deve ter pelo menos 2 alternativas corretas");
        }

        if (correctCount == options.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade de múltipla escolha deve ter pelo menos 1 alternativa incorreta");
        }
    }

    private int validateCommonOptionsRules(List<OptionDTO> options, String statement,
                                          int minAlternatives, int maxAlternatives) {
        if (options == null || options.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "As alternativas são obrigatórias");
        }

        if (options.size() < minAlternatives || options.size() > maxAlternatives) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade deve ter no mínimo " + minAlternatives + " e no máximo " + maxAlternatives + " alternativas");
        }

        Set<String> uniqueOptions = new HashSet<>();
        int correctCount = 0;

        for (OptionDTO option : options) {
            String optionText = option.getText().trim();

            if (optionText.length() < 4 || optionText.length() > 80) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas devem ter no mínimo 4 e no máximo 80 caracteres");
            }

            if (optionText.equalsIgnoreCase(statement.trim())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas não podem ser iguais ao enunciado da atividade");
            }

            if (!uniqueOptions.add(optionText.toLowerCase())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas não podem ser iguais entre si");
            }

            if (option.isCorrect()) {
                correctCount++;
            }
        }

        return correctCount;
    }

    private void validateAndHandleOrderSequence(Long courseId, Integer newOrder) {
        Integer maxOrder = taskRepository.findMaxOrderByCourseId(courseId);

        if (maxOrder == null) {
            if (newOrder != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A primeira atividade deve ter ordem 1");
            }
            return;
        }

        if (newOrder > maxOrder + 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("A ordem das atividades deve ser contínua, sem saltos. " +
                            "A maior ordem atual é %d. Você pode adicionar uma atividade com ordem entre 1 e %d.",
                            maxOrder, maxOrder + 1));
        }

        if (taskRepository.existsByCourseIdAndOrderNumber(courseId, newOrder)) {
            shiftTasksOrder(courseId, newOrder);
        }
    }

    private void shiftTasksOrder(Long courseId, Integer fromOrder) {
        List<Task> tasksToShift = taskRepository.findByCourseIdAndOrderNumberGreaterThanEqual(courseId, fromOrder);

        for (int i = tasksToShift.size() - 1; i >= 0; i--) {
            Task task = tasksToShift.get(i);
            task.setOrderNumber(task.getOrderNumber() + 1);
            taskRepository.save(task);
        }
    }
}

