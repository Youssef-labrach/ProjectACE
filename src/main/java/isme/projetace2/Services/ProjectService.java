package isme.projetace2.Services;

import isme.projetace2.Models.Project;
import isme.projetace2.Repos.ProjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepo;

    public List<Project> getAllProjects() {
        return projectRepo.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepo.findById(id);
    }

    public Project saveProject(Project project) {
        return projectRepo.save(project);
    }

    public void deleteProject(Long id) {
        projectRepo.deleteById(id);
    }
}

