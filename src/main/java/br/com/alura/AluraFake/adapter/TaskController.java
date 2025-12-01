package br.com.alura.AluraFake.adapter;

import br.com.alura.AluraFake.infra.dto.task.SingleChoiceTaskRequestDTO;
import br.com.alura.AluraFake.infra.dto.task.TaskRequestDTO;
import br.com.alura.AluraFake.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {

    @Autowired
    private TaskService taskService;

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