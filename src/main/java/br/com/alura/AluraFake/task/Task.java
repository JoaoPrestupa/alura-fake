package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, length = 255)
    private String statement;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Deprecated
    public Task() {}

    public Task(String statement, Integer orderNumber, Type type, Course course) {
        this.statement = statement;
        this.orderNumber = orderNumber;
        this.type = type;
        this.course = course;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatement() {
        return statement;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public Type getType() {
        return type;
    }

    public Course getCourse() {
        return course;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }
}



