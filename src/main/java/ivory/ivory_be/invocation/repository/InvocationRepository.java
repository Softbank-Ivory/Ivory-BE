package ivory.ivory_be.invocation.repository;

import ivory.ivory_be.invocation.entity.Invocation;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvocationRepository extends JpaRepository<Invocation, Long> {

    @Modifying
    @Query("UPDATE Invocation i SET i.status = :status, i.updatedAt = :updatedAt WHERE i.invocationId = :invocationId")
    void updateStatusByInvocationId(
            @Param("invocationId") String invocationId,
            @Param("status") String status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
