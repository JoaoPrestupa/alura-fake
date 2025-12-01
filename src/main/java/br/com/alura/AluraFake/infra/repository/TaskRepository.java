package br.com.alura.AluraFake.infra.repository;

import br.com.alura.AluraFake.infra.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByCourseIdAndStatement(Long courseId, String statement);

    @Query("SELECT MAX(t.orderNumber) FROM Task t WHERE t.course.id = :courseId")
    Integer findMaxOrderByCourseId(Long courseId);

    @Query("SELECT t FROM Task t WHERE t.course.id = :courseId AND t.orderNumber >= :orderNumber ORDER BY t.orderNumber ASC")
    List<Task> findByCourseIdAndOrderNumberGreaterThanEqual(@Param("courseId") Long courseId,
                                                             @Param("orderNumber") Integer orderNumber);

    @Modifying
    @Query("UPDATE Task t SET t.orderNumber = t.orderNumber + 1 WHERE t.course.id = :courseId AND t.orderNumber >= :fromOrder")
    void shiftOrdersFrom(@Param("courseId") Long courseId, @Param("fromOrder") Integer fromOrder);

    boolean existsByCourseIdAndOrderNumber(Long courseId, Integer orderNumber);

    List<Task> findByCourseIdOrderByOrderNumberAsc(Long courseId);

    long countByCourseId(Long courseId);
}

