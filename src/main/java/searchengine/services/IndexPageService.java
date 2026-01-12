package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.ModelPage;
import searchengine.model.ModelSite;
import searchengine.model.StatusEnum;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.stemmer.RussianStemmer;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;

@Service
public class IndexPageService {

    @Autowired
    private SitesList sitesList;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private RussianStemmer stemmer;

    @Autowired
    private LemmaService lemmaService;

    @Transactional
    public boolean indexPage(String url){
        try {
            System.out.println("Индексация страницы " + url);
            Site siteCheck = null;
            for (Site site : sitesList.getSites()){
                if (url.startsWith(site.getUrl())){
                    siteCheck = site;
                    break;
                }
            }
            if (siteCheck == null){
                System.out.println("Страница не найдена на сайтах конфигурации");
                return false;
            }

            ModelSite modelSite = siteRepository.findByUrl(siteCheck.getUrl());
            if (modelSite == null){
                System.out.println("Создание новой записи в БД");
                modelSite = new ModelSite();
                modelSite.setUrl(siteCheck.getUrl());
                modelSite.setName(siteCheck.getName());
                modelSite.setStatusTime(LocalDateTime.now());
                modelSite.setStatus(StatusEnum.INDEXED);
                siteRepository.save(modelSite);
            }

            Connection.Response con = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();

            if (!(con.statusCode() == 200)){
                System.out.println("Ошибка подключения -" + con.statusCode());
                return false;
            }
            String path = normalizerUrl(url);

            ModelPage checkPageInDb = pageRepository.findByPathAndSite(path, modelSite);
            if (checkPageInDb != null){
                deletePageData(checkPageInDb);
            }

            ModelPage modelPage = new ModelPage();
            modelPage.setSite(modelSite);
            modelPage.setPath(path);
            modelPage.setCode(con.statusCode());
            modelPage.setContent(con.parse().html());
            pageRepository.save(modelPage);

            lemmaService.indexPage(modelPage);

            System.out.println("Индексация страницы " + url + " завершена" );
            return true;

        }
        catch (Exception e){
            System.out.println("Возникла ошибка индексации " + e.getMessage());
            return false;
        }
    }

    public String normalizerUrl(String url){
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            String query = uri.getQuery();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            if (query != null && !query.isEmpty()) {
                path = path + "?" + query;
            }
            return path;


        }catch (Exception e){
            return "/";
        }
    }

    @Transactional
    private void deletePageData(ModelPage page) { // Удаляем старые данные (леммы и индексы)
        indexRepository.deleteByPage(page);
        pageRepository.delete(page);
    }



}
