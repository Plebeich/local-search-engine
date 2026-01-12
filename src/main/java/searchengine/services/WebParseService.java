package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.ModelPage;
import searchengine.model.ModelSite;
import searchengine.model.StatusEnum;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import javax.persistence.EntityManagerFactory;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


@Service
public class WebParseService {
    @Autowired
    private SitesList sitesList;
    @Autowired
    private EntityManagerFactory emFactory;
    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaService lemmaService;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    private volatile boolean indexingStatus = false;
    private ForkJoinPool forkJoinPool;


    public void startIndexing() {
        if (indexingStatus){
            throw new RuntimeException("Индексация уже запущена");
        }
        indexingStatus = true;
        clearDB();

        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        List<Site> sites = sitesList.getSites();
        for (Site site : sites){

            try {
                ModelSite modelSite = new ModelSite();
                modelSite.setName(site.getName());
                modelSite.setUrl(site.getUrl());
                modelSite.setStatus(StatusEnum.INDEXING);
                modelSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(modelSite);
                Thread thread = new Thread(() -> startParsingSite(site,modelSite));
                thread.start();

            }catch (Exception e) {
                throw new RuntimeException("Ошибка при сохранении сайта: " + site.getName(), e);
            }
        }
    }

    private void startParsingSite(Site site, ModelSite modelSite){

        try {
            boolean mainPageSaved = saveMainPage(site, modelSite);
            if (!mainPageSaved) {
                throw new RuntimeException("Не удалось сохранить главную страницу сайта: " + site.getUrl());
            }

            System.out.println("Главная страница сохранена для сайта: " + site.getName());
            List<String> siteLinks = parsingSite(site);
            System.out.println("Найдено ссылок для " + site.getName() + ": " + siteLinks.size());

            if (!siteLinks.isEmpty()) {
                TaskIndexing task = new TaskIndexing(siteLinks,modelSite,emFactory,lemmaService);
                forkJoinPool.execute(task);
                task.join();
            }

            modelSite.setStatus(StatusEnum.INDEXED);
            modelSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(modelSite);

            System.out.println("Индексация завершена для сайта: " + site.getName());
        }catch (Exception e){
            System.err.println("Ошибка при индексации сайта " + site.getName() + ": " + e.getMessage());
            e.printStackTrace();
            if (siteRepository.existsById(modelSite.getId())){
                modelSite.setLastError("ошибка индексации" + e.getMessage());
                modelSite.setStatusTime(LocalDateTime.now());
                modelSite.setStatus(StatusEnum.FAILED);
                siteRepository.save(modelSite);
            }
        }
    }

    private boolean saveMainPage(Site site, ModelSite modelSite) {
        try {
            System.out.println("Начинаю сохранение главной страницы для: " + site.getUrl());
            Connection.Response connection = Jsoup.connect(site.getUrl())
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = connection.statusCode();
            System.out.println("Код ответа для " + site.getUrl() + ": " + statusCode);

            ModelPage modelPage = new ModelPage();
            modelPage.setSite(modelSite);
            modelPage.setPath(site.getUrl());
            modelPage.setContent(connection.parse().html());
            modelPage.setCode(connection.statusCode());
            pageRepository.save(modelPage);

            System.out.println("Главная страница успешно сохранена для: " + site.getUrl());
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении главной страницы " + site.getUrl() + ": " + e.getMessage());
            e.printStackTrace();
            
           try {
               ModelPage modelPage = new ModelPage();
               modelPage.setSite(modelSite);
               modelPage.setCode(500);
               modelPage.setPath(site.getUrl());
               modelPage.setContent("");
               pageRepository.save(modelPage);
               return true;

           }catch (Exception saveEx){
               System.out.println("не удалось сохранить страницу с ошибкой" + saveEx.getMessage());
               return false;
           }
        }
    }

    private List<String> parsingSite(Site site){
        List<String> trueLinks = new ArrayList<>();
        try {
            System.out.println("Начинаю парсинг ссылок для: " + site.getUrl());
            Document doc = Jsoup.connect(site.getUrl())
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .get();
            Elements links = doc.select("a[href]");
            System.out.println("Найдено ссылок на странице " + site.getUrl() + ": " + links.size());
            trueLinks = linksFilter(links, site);
            System.out.println("Отфильтровано ссылок для " + site.getUrl() + ": " + trueLinks.size());
        }catch (Exception e){
            System.err.println("Сайт - " + site.getUrl() + " недоступен! ошибка - " + e.getMessage());
            e.printStackTrace();
        }
        return trueLinks;
    }

    public List<String> linksFilter(Elements links, Site site ){
        List<String> result = new ArrayList<>();
        try {
            URI baseUri = new URI(site.getUrl());
            String baseDomain = baseUri.getHost();
            String normalizedBaseDomain = baseDomain != null ? baseDomain.replaceFirst("^www\\.", "") : "";
            String baseScheme = baseUri.getScheme();
            
            System.out.println("Базовый домен для фильтрации: " + normalizedBaseDomain);
            
            for (Element link : links) {
                try {
                    String href = link.attr("href");

                    if (href.isEmpty() || href.startsWith("#") ||
                            href.startsWith("tel:") || href.startsWith("mailto:") ||
                            href.startsWith("javascript:") || href.matches(".*\\.(pdf|jpg|png|zip|gif|css|js|ico|svg)$")) {
                        continue;
                    }
                    
                    String absoluteUrl = link.absUrl("href");
                    if (absoluteUrl == null || absoluteUrl.isEmpty()) {
                        continue;
                    }

                    try {
                        URI linkUri = new URI(absoluteUrl);
                        String linkHost = linkUri.getHost();
                        
                        if (linkHost == null) {
                            if (!absoluteUrl.startsWith("http")) {
                                absoluteUrl = baseScheme + "://" + baseDomain + (absoluteUrl.startsWith("/") ? "" : "/") + absoluteUrl;
                                linkUri = new URI(absoluteUrl);
                                linkHost = linkUri.getHost();
                            }
                        }
                        
                        if (linkHost != null) {
                            String normalizedLinkHost = linkHost.replaceFirst("^www\\.", "");

                            if (normalizedLinkHost.equals(normalizedBaseDomain)) {
                                String cleanUrl = absoluteUrl.split("#")[0];
                                if (!result.contains(cleanUrl)) {
                                    result.add(cleanUrl);
                                }
                            }
                        }
                    } catch (Exception uriEx) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при фильтрации ссылок: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public void clearDB(){
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();

    }

    public void stopIndexing() {
        indexingStatus = false;
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
        }
    }

    public boolean indexingStatusCheck() {
        return indexingStatus;
    }

}
