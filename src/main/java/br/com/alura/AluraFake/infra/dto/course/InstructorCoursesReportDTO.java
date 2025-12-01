package br.com.alura.AluraFake.infra.dto.course;

import java.util.List;

public class InstructorCoursesReportDTO {
    private List<InstructorCourseDTO> courses;
    private long totalPublishedCourses;

    public InstructorCoursesReportDTO(List<InstructorCourseDTO> courses, long totalPublishedCourses) {
        this.courses = courses;
        this.totalPublishedCourses = totalPublishedCourses;
    }

    public List<InstructorCourseDTO> getCourses() {
        return courses;
    }

    public long getTotalPublishedCourses() {
        return totalPublishedCourses;
    }
}

