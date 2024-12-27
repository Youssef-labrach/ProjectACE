package isme.service_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Value("${service.user.url}")
    private String userServiceUrl;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> getProjectsByUserId(Long userId) {
        return projectRepository.findByUserId(userId);
    }

    public Project saveProject(Project project) {
        // Vérifie si l'utilisateur existe dans UserService
        if (!userExists(project.getUserId())) {
            throw new RuntimeException("User not found with ID: " + project.getUserId());
        }

        return projectRepository.save(project);
    }

    private boolean userExists(Long userId) {
        String url = userServiceUrl + "/" + userId;
        try {
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false; // Returns false if the user does not exist or an error occurs
        }
    }
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}