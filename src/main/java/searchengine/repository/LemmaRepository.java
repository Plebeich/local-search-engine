package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelLemma;
import searchengine.model.ModelSite;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<ModelLemma, Integer> {
    ModelLemma findByLemmaAndSite(String lemma, ModelSite site);
    List<ModelLemma> findBySite(ModelSite site);
    int countBySite(ModelSite site);
}