package br.com.alura.AluraFake.infra.repository;

import br.com.alura.AluraFake.infra.enumerated.Status;
import br.com.alura.AluraFake.infra.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>{

    List<Course> findByInstructorId(Long instructorId);

    long countByInstructorIdAndStatus(Long instructorId, Status status);
}
