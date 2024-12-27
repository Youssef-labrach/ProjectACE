package isme.service_algorithm;

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
        return algorithmService.detectAlgorithms(projectPath, projectId);
    }
}
