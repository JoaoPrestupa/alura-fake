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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;

    public TaskService(CourseRepository courseRepository, TaskRepository taskRepository) {
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
    }

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
        // TODO: Salvar as alternativas em uma entidade separada
    }

    @Transactional
    public void createMultipleChoiceTask(SingleChoiceTaskRequestDTO dto) {
        Course course = validateAndGetCourse(dto.getCourseId(), dto.getStatement());
        validateMultipleChoiceOptions(dto.getOptions(), dto.getStatement());
        validateAndHandleOrderSequence(dto.getCourseId(), dto.getOrder());

        Task task = new Task(dto.getStatement(), dto.getOrder(), Type.MULTIPLE_CHOICE, course);
        taskRepository.save(task);
        // TODO: Salvar as alternativas em uma entidade separada
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

    private void validateOptions(List<OptionDTO> options, String statement, boolean isMultipleChoice, Integer minAlternatives,
                                 Integer maxAlternatives, Integer sizeMinAlternative, Integer sizeMaxAlternative,
                                 Integer correctAlternativesMin) {
        if (options == null || options.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "As alternativas são obrigatórias");
        }

        // Valida quantidade de alternativas
        if (options.size() < minAlternatives || options.size() > maxAlternatives) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade deve ter no mínimo" + minAlternatives + " e no máximo " + maxAlternatives + " alternativas");
        }

        Set<String> uniqueOptions = new HashSet<>();
        int correctCount = 0;

        for (OptionDTO option : options) {
            String optionText = option.getText().trim();

            // Valida tamanho da alternativa
            if (optionText.length() < sizeMinAlternative || optionText.length() > sizeMaxAlternative) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas devem ter no mínimo " + sizeMinAlternative + " e no máximo " + sizeMaxAlternative + " caracteres");
            }

            // Verifica se a alternativa é igual ao enunciado
            if (optionText.equalsIgnoreCase(statement.trim())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas não podem ser iguais ao enunciado da atividade");
            }

            // Verifica duplicatas (case-insensitive)
            if (!uniqueOptions.add(optionText.toLowerCase())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas não podem ser iguais entre si");
            }

            if (option.isCorrect()) {
                correctCount++;
            }
        }

        // Valida quantidade de alternativas corretas
        if (isMultipleChoice) {
            if (correctCount < correctAlternativesMin) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A atividade de múltipla escolha deve ter pelo menos 2 alternativas corretas");
            }
            if (correctCount == options.size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A atividade de múltipla escolha deve ter pelo menos 1 alternativa incorreta");
            }
        } else {
            if (correctCount != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A atividade deve ter uma única alternativa correta");
            }
        }
    }

    private void validateSingleChoiceOptions(List<OptionDTO> options, String statement) {
        // Validações comuns a todas as atividades com alternativas
        int correctCount = validateCommonOptionsRules(options, statement, 2, 5);

        // Validação específica: deve ter exatamente 1 alternativa correta
        if (correctCount != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade deve ter uma única alternativa correta");
        }
    }

    private void validateMultipleChoiceOptions(List<OptionDTO> options, String statement) {
        // Validações comuns a todas as atividades com alternativas
        int correctCount = validateCommonOptionsRules(options, statement, 2, 5);

        // Validações específicas de múltipla escolha
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

    /**
     * Valida as regras comuns a todas as atividades com alternativas.
     * Retorna a quantidade de alternativas corretas.
     */
    private int validateCommonOptionsRules(List<OptionDTO> options, String statement,
                                          int minAlternatives, int maxAlternatives) {
        if (options == null || options.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "As alternativas são obrigatórias");
        }

        // Valida quantidade de alternativas
        if (options.size() < minAlternatives || options.size() > maxAlternatives) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A atividade deve ter no mínimo " + minAlternatives + " e no máximo " + maxAlternatives + " alternativas");
        }

        Set<String> uniqueOptions = new HashSet<>();
        int correctCount = 0;

        for (OptionDTO option : options) {
            String optionText = option.getText().trim();

            // Valida tamanho da alternativa (4-80 caracteres)
            if (optionText.length() < 4 || optionText.length() > 80) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas devem ter no mínimo 4 e no máximo 80 caracteres");
            }

            // Verifica se a alternativa é igual ao enunciado
            if (optionText.equalsIgnoreCase(statement.trim())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As alternativas não podem ser iguais ao enunciado da atividade");
            }

            // Verifica duplicatas (case-insensitive)
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

    /**
     * Valida a sequência de ordem das atividades e desloca as atividades existentes se necessário.
     *
     * Regras:
     * 1. A ordem deve ser sequencial, sem saltos (não pode adicionar ordem 4 se não existe ordem 3)
     * 2. Se a ordem já existe, todas as atividades com aquela ordem ou superiores são deslocadas +1
     *
     * @param courseId ID do curso
     * @param newOrder Ordem da nova atividade
     */
    private void validateAndHandleOrderSequence(Long courseId, Integer newOrder) {
        Integer maxOrder = taskRepository.findMaxOrderByCourseId(courseId);

        // Se é a primeira atividade do curso
        if (maxOrder == null) {
            if (newOrder != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A primeira atividade deve ter ordem 1");
            }
            return;
        }

        // Valida que não há saltos na sequência
        if (newOrder > maxOrder + 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("A ordem das atividades deve ser contínua, sem saltos. " +
                            "A maior ordem atual é %d. Você pode adicionar uma atividade com ordem entre 1 e %d.",
                            maxOrder, maxOrder + 1));
        }

        // Se a ordem é válida mas já existe, desloca as atividades existentes
        if (taskRepository.existsByCourseIdAndOrderNumber(courseId, newOrder)) {
            shiftTasksOrder(courseId, newOrder);
        }
    }

    /**
     * Desloca todas as atividades com ordem >= fromOrder em +1 posição.
     *
     * Exemplo:
     * Antes: [1: A, 2: B, 3: C]
     * Inserir nova em ordem 2
     * Depois: [1: A, 2: Nova, 3: B, 4: C]
     *
     * @param courseId ID do curso
     * @param fromOrder Ordem a partir da qual deslocar
     */
    private void shiftTasksOrder(Long courseId, Integer fromOrder) {
        // Busca todas as atividades que precisam ser deslocadas (ordem >= fromOrder)
        List<Task> tasksToShift = taskRepository.findByCourseIdAndOrderNumberGreaterThanEqual(courseId, fromOrder);

        // Desloca cada uma em +1 posição (em ordem decrescente para evitar conflitos de chave única)
        for (int i = tasksToShift.size() - 1; i >= 0; i--) {
            Task task = tasksToShift.get(i);
            task.setOrderNumber(task.getOrderNumber() + 1);
            taskRepository.save(task);
        }
    }
}

