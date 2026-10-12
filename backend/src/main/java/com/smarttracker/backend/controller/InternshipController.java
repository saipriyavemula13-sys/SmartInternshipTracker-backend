
package com.smarttracker.backend.controller;

import com.smarttracker.backend.entity.Internship;
import com.smarttracker.backend.entity.User;
import com.smarttracker.backend.repository.InternshipRepository;
import com.smarttracker.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

    public InternshipController(
            InternshipRepository repository,
            UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User getLoggedInUser(Authentication authentication) {
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

    @GetMapping
    public List<Internship> getAllInternships(
            Authentication authentication) {
        User user = getLoggedInUser(authentication);
        return repository.findAllByUser_Email(user.getEmail());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Internship createInternship(
            @RequestBody Internship internship,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        internship.setUser(user);

        if (internship.getStatus() == null
                || internship.getStatus().isBlank()) {
            internship.setStatus("Applied");
        }

        return repository.save(internship);
    }

    @PutMapping("/{id}")
    public Internship updateInternship(
            @PathVariable Long id,
            @RequestBody Internship updatedInternship,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        Internship existing = getOwnedInternship(id, user);

        existing.setCompanyName(updatedInternship.getCompanyName());
        existing.setRole(updatedInternship.getRole());
        existing.setLocation(updatedInternship.getLocation());
        existing.setApplicationLink(updatedInternship.getApplicationLink());
        existing.setApplicationDate(updatedInternship.getApplicationDate());
        existing.setDeadline(updatedInternship.getDeadline());
        existing.setInterviewDate(updatedInternship.getInterviewDate());
        existing.setFollowUpDate(updatedInternship.getFollowUpDate());
        existing.setStatus(updatedInternship.getStatus());
        existing.setNotes(updatedInternship.getNotes());

        return repository.save(existing);
    }

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