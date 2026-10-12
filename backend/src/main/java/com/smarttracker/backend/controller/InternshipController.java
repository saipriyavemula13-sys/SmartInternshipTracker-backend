
package com.smarttracker.backend.controller;

import com.smarttracker.backend.entity.Internship;
import com.smarttracker.backend.entity.InternshipStatusHistory;
import com.smarttracker.backend.entity.User;

import com.smarttracker.backend.repository.InternshipRepository;
import com.smarttracker.backend.repository.InternshipStatusHistoryRepository;
import com.smarttracker.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/internships")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:3001"
})
public class InternshipController {

    private final InternshipRepository repository;
    private final UserRepository userRepository;
    private final InternshipStatusHistoryRepository historyRepository;

    public InternshipController(
            InternshipRepository repository,
            UserRepository userRepository,
            InternshipStatusHistoryRepository historyRepository) {

        this.repository = repository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Internship getOwnedInternship(Long id, User user) {
        Internship internship = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Internship not found"));

        if (internship.getUser() == null
                || !internship.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Internship not found");
        }

        return internship;
    }

    private void recordStatusChange(
            Internship internship,
            User user,
            String previousStatus,
            String newStatus) {

        InternshipStatusHistory history =
                new InternshipStatusHistory();

        history.setInternshipId(internship.getId());
        history.setUserId(user.getId());
        history.setCompanyName(internship.getCompanyName());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);

        historyRepository.save(history);
    }

    // Get all internships belonging to the logged-in user
    @GetMapping
    public List<Internship> getAllInternships(
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        return repository.findAllByUser_Email(user.getEmail());
    }

    // Create an internship and record its initial status
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Internship createInternship(
            @RequestBody Internship internship,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        internship.setUser(user);

        if (internship.getStatus() == null
                || internship.getStatus().isBlank()) {
            internship.setStatus("Applied");
        }

        Internship saved = repository.save(internship);

        recordStatusChange(
            saved,
            user,
            null,
            saved.getStatus()
        );

        return saved;
    }

    // Update an internship and record actual status changes
    @PutMapping("/{id}")
    @Transactional
    public Internship updateInternship(
            @PathVariable Long id,
            @RequestBody Internship updatedInternship,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Internship existing = getOwnedInternship(id, user);

        String previousStatus = existing.getStatus();
        String requestedStatus = updatedInternship.getStatus();

        existing.setCompanyName(updatedInternship.getCompanyName());
        existing.setRole(updatedInternship.getRole());
        existing.setLocation(updatedInternship.getLocation());
        existing.setApplicationLink(updatedInternship.getApplicationLink());
        existing.setApplicationDate(updatedInternship.getApplicationDate());
        existing.setDeadline(updatedInternship.getDeadline());
        existing.setInterviewDate(updatedInternship.getInterviewDate());
        existing.setFollowUpDate(updatedInternship.getFollowUpDate());
        existing.setNotes(updatedInternship.getNotes());

        // Preserve the existing status if the request omits it.
        if (requestedStatus != null && !requestedStatus.isBlank()) {
            existing.setStatus(requestedStatus);
        }

        Internship saved = repository.save(existing);

        String newStatus = saved.getStatus();

        if (newStatus != null
                && !newStatus.isBlank()
                && !newStatus.equals(previousStatus)) {

            recordStatusChange(
                saved,
                user,
                previousStatus,
                newStatus
            );
        }

        return saved;
    }

    // Get all status history for the logged-in user
    @GetMapping("/history")
    public List<InternshipStatusHistory> getAllHistory(
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        return historyRepository
            .findAllByUserIdOrderByChangedAtDesc(user.getId());
    }

    // Get status history for one internship owned by the user
    @GetMapping("/{id}/history")
    public List<InternshipStatusHistory> getInternshipHistory(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        getOwnedInternship(id, user);

        return historyRepository
            .findAllByUserIdAndInternshipIdOrderByChangedAtDesc(
                user.getId(), id);
    }

    // Delete an internship owned by the logged-in user
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInternship(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Internship existing = getOwnedInternship(id, user);

        repository.delete(existing);
    }
}