
package com.smarttracker.backend.repository;

import com.smarttracker.backend.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipRepository
        extends JpaRepository<Internship, Long> {

    List<Internship> findAllByUser_Email(String email);
}