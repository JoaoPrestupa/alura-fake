package br.com.alura.AluraFake.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>{

    List<Course> findByInstructorId(Long instructorId);

    long countByInstructorIdAndStatus(Long instructorId, Status status);
}
