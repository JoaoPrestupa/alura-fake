package br.com.alura.AluraFake.infra.dto.task;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskRequestDTO {

    @NotNull(message = "O ID do curso é obrigatório")
    @Positive(message = "O ID do curso deve ser positivo")
    private Long courseId;

    @NotBlank(message = "O enunciado é obrigatório")
    @Size(min = 4, max = 255, message = "O enunciado deve ter entre 4 e 255 caracteres")
    private String statement;

    @NotNull(message = "A ordem é obrigatória")
    @Positive(message = "A ordem deve ser um número inteiro positivo")
    private Integer order;
}
