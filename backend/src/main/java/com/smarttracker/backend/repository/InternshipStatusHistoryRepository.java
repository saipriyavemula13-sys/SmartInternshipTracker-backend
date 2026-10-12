
package com.smarttracker.backend.repository;

import com.smarttracker.backend.entity.InternshipStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipStatusHistoryRepository
        extends JpaRepository<InternshipStatusHistory, Long> {

    List<InternshipStatusHistory>
        findAllByUserIdAndInternshipIdOrderByChangedAtDesc(
            Long userId, Long internshipId);

    List<InternshipStatusHistory>
        findAllByUserIdOrderByChangedAtDesc(Long userId);
}
