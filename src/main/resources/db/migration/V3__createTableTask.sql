CREATE TABLE task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statement VARCHAR(255) NOT NULL,
    order_number INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    course_id BIGINT NOT NULL,
    CONSTRAINT fk_task_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE INDEX idx_task_course_id ON task(course_id);
CREATE INDEX idx_task_course_statement ON task(course_id, statement);

