package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelIndex;
import searchengine.model.ModelLemma;
import searchengine.model.ModelPage;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<ModelIndex, Integer> {
    List<ModelIndex> findByLemma(ModelLemma lemma);
    List<ModelIndex> findByPage(ModelPage page);
    void deleteByPage(ModelPage page);
    ModelIndex findByLemmaAndPage(ModelLemma lemma, ModelPage page);
}