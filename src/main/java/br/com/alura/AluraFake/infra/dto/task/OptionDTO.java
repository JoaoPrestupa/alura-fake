package br.com.alura.AluraFake.infra.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OptionDTO {

    @NotBlank(message = "A alternativa não pode ser vazia")
    @Size(min = 4, max = 80, message = "A alternativa deve ter entre 4 e 80 caracteres")
    private String text;

    private boolean correct;
}

