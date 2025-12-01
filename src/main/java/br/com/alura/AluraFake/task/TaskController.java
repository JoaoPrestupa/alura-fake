package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.task.dto.request.SingleChoiceTaskRequestDTO;
import br.com.alura.AluraFake.task.dto.request.TaskRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Atividade 1.1
    @PostMapping("/task/new/opentext")
    public ResponseEntity<Void> newOpenTextExercise(@RequestBody @Valid TaskRequestDTO data) {
        taskService.createOpenTextTask(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/task/new/singlechoice")
    public ResponseEntity<Void> newSingleChoice(@RequestBody @Valid SingleChoiceTaskRequestDTO data) {
        taskService.createSingleChoiceTask(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/task/new/multiplechoice")
    public ResponseEntity<Void> newMultipleChoice(@RequestBody @Valid SingleChoiceTaskRequestDTO data) {
        taskService.createMultipleChoiceTask(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}