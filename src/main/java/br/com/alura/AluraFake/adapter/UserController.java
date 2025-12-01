package br.com.alura.AluraFake.adapter;

import br.com.alura.AluraFake.infra.dto.user.NewUserDTO;
import br.com.alura.AluraFake.infra.entity.User;
import br.com.alura.AluraFake.service.CourseService;
import br.com.alura.AluraFake.infra.dto.course.InstructorCoursesReportDTO;
import br.com.alura.AluraFake.infra.dto.user.UserListItemDTO;
import br.com.alura.AluraFake.infra.repository.UserRepository;
import br.com.alura.AluraFake.util.ErrorItemDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseService courseService;

    @Transactional
    @PostMapping("/user/new")
    public ResponseEntity newStudent(@RequestBody @Valid NewUserDTO newUser) {
        if(userRepository.existsByEmail(newUser.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorItemDTO("email", "Email já cadastrado no sistema"));
        }
        User user = newUser.toModel();
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/user/all")
    public List<UserListItemDTO> listAllUsers() {
        return userRepository.findAll().stream().map(UserListItemDTO::new).toList();
    }

    @GetMapping("/instructor/{id}/courses")
    public ResponseEntity<InstructorCoursesReportDTO> getInstructorCourses(@PathVariable("id") Long id) {
        InstructorCoursesReportDTO report = courseService.generateInstructorReport(id);
        return ResponseEntity.ok(report);
    }

}
