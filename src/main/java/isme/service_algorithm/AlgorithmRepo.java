package isme.service_algorithm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlgorithmRepo extends JpaRepository<Algorithm, Long> {
    List<Algorithm> findByProjectId(Long projectId);
}
