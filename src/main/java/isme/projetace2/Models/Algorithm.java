package isme.projetace2.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
public class Algorithm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 5000)
    private String code;
    private String recommendation;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Project project;

    public Algorithm() {
    }

    public Algorithm(Long id, String code, String name, Project project, String recommendation) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.project = project;
        this.recommendation = recommendation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
