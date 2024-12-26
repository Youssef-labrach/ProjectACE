package isme.projetace2.Repos;

import isme.projetace2.Models.Algorithm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgorithmRepo extends JpaRepository<Algorithm, Long> {
}
