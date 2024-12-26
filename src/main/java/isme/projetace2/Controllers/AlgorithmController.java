package isme.projetace2.Controllers;

import isme.projetace2.Models.Project;
import isme.projetace2.Services.AlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    @Autowired
    private AlgorithmService algorithmService;

    @PostMapping("/detect")
    public List<String> detectAlgorithms(@RequestParam String projectPath, @RequestParam Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return algorithmService.detectAlgorithms(projectPath, project);
    }
}