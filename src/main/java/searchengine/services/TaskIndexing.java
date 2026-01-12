package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.model.ModelPage;
import searchengine.model.ModelSite;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class TaskIndexing extends RecursiveAction {
    private final List<String> pages;
    private final ModelSite modelSite;
    private final EntityManagerFactory emf;
    private final LemmaService lemmaService;

    public TaskIndexing(List<String> pages, ModelSite modelSite, EntityManagerFactory emf, LemmaService lemmaService) {
        this.pages = pages;
        this.modelSite = modelSite;
        this.emf = emf;
        this.lemmaService = lemmaService;
    }

    @Override
    protected void compute() {
        List<String> passedLink = new ArrayList<>();
        for (String page : pages){
            if (passedLink.contains(page))continue;
            EntityManager em = null;
            try {
                System.out.println("парсинг страницы: " + page);
                Connection.Response connection = Jsoup.connect(page)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .timeout(1000)
                        .ignoreHttpErrors(true)
                        .execute();

                em = emf.createEntityManager();
                em.getTransaction().begin();

                ModelPage modelPage = new ModelPage();
                modelPage.setPath(page);
                modelPage.setSite(modelSite);
                modelPage.setContent(connection.parse().html());
                modelPage.setCode(connection.statusCode());
                em.persist(modelPage);
                em.getTransaction().commit();
                em.close();

                int statusCode = connection.statusCode();

                if (statusCode == 200){
                    lemmaService.indexPage(modelPage);
                }

            }catch (Exception e){
                if (em != null && em.isOpen()) {
                    try {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                    } catch (Exception rollbackEx) {
                        System.out.println("Ошибка при rollback: " + rollbackEx.getMessage());
                    }
                }

                EntityManager emError = null;
                try {
                    System.out.println("ошибка парсинга страницы: " + page + " - " + e.getMessage());
                    emError = emf.createEntityManager();
                    emError.getTransaction().begin();
                    ModelPage modelPage = new ModelPage();
                    modelPage.setPath(page);
                    modelPage.setSite(modelSite);
                    modelPage.setContent("ошибка");
                    modelPage.setCode(500);
                    emError.persist(modelPage);
                    emError.getTransaction().commit();
                } catch (Exception saveErrorEx) {
                    if (emError != null && emError.getTransaction().isActive()) {
                        emError.getTransaction().rollback();
                    }
                    System.out.println("Ошибка при сохранении ошибки парсинга: " + saveErrorEx.getMessage());
                } finally {
                    if (emError != null && emError.isOpen()) {
                        emError.close();
                    }
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            passedLink.add(page);
        }
    }
}
