package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.ModelPage;
import searchengine.model.ModelSite;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<ModelPage, Integer> {
    List<ModelPage> findBySite(ModelSite site);
    ModelPage findByPathAndSite(String path, ModelSite site);
    int countBySite(ModelSite site);

}