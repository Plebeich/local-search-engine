package searchengine.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchStatistics;
import searchengine.model.ModelIndex;
import searchengine.model.ModelLemma;
import searchengine.model.ModelPage;
import searchengine.model.ModelSite;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.stemmer.RussianStemmer;

import java.util.*;

@Service
public class SearchService {
    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private RussianStemmer stemmer;

    public List<SearchStatistics> search(String query, String siteUrl, int offset, int limit) {
        List<SearchStatistics> results = new ArrayList<>();
        try {
            String[] queryStems = stemmer.getQueryStems(query);

            if (queryStems.length == 0) {
                return results;
            }

            List<ModelSite> sitesToSearch;
            if (siteUrl != null) { //
                ModelSite site = siteRepository.findByUrl(siteUrl);
                sitesToSearch = (site != null) ? Collections.singletonList(site) : new ArrayList<>();
            } else {
                sitesToSearch = siteRepository.findAll();
            }

            for (ModelSite site : sitesToSearch) {
                List<ModelLemma> foundLemmas = new ArrayList<>();
                for (String stem : queryStems) {
                    ModelLemma lemma = lemmaRepository.findByLemmaAndSite(stem, site);
                    if (lemma != null) {
                        foundLemmas.add(lemma);
                    }
                }

                if (foundLemmas.isEmpty()) {
                    continue;
                }

                long totalPages = pageRepository.countBySite(site);
                List<ModelLemma> filteredLemmas = new ArrayList<>();
                for (ModelLemma lemma : foundLemmas) {
                    double frequencyPercent = (double) lemma.getFrequency() / totalPages * 100;
                    if (frequencyPercent < 80) { // Экспериментальное значение
                        filteredLemmas.add(lemma);
                    }
                }

                if (filteredLemmas.isEmpty()) {
                    continue;
                }

                filteredLemmas.sort(Comparator.comparingInt(ModelLemma::getFrequency));
                List<ModelPage> foundPages = PagesAllLemmas(filteredLemmas);
                Map<ModelPage, Float> relevanceMap = calculateRelevance(foundPages, filteredLemmas);
                List<Map.Entry<ModelPage, Float>> sortedEntries = new ArrayList<>(relevanceMap.entrySet());
                sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                for (int i = offset; i < Math.min(sortedEntries.size(), offset + limit); i++) {
                    Map.Entry<ModelPage, Float> entry = sortedEntries.get(i);
                    ModelPage page = entry.getKey();

                    SearchStatistics result = new SearchStatistics();
                    result.setSite(site.getUrl());
                    result.setSiteName(site.getName());
                    result.setUri(page.getPath());
                    result.setTitle(stemmer.extractTitle(page.getContent()));
                    result.setSnippet(createSnippet(page.getContent(), queryStems));
                    result.setRelevance(entry.getValue());

                    results.add(result);
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка при поиске: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }



    private List<ModelPage> PagesAllLemmas(List<ModelLemma> lemmas) {
        if (lemmas.isEmpty()) {
            return new ArrayList<>();
        }

        ModelLemma firstLemma = lemmas.get(0);
        List<ModelIndex> indexes = indexRepository.findByLemma(firstLemma);
        Set<ModelPage> pages = new HashSet<>();
        for (ModelIndex index : indexes) {
            pages.add(index.getPage());
        }

        for (int i = 1; i < lemmas.size(); i++) {
            ModelLemma lemma = lemmas.get(i);
            Set<ModelPage> tempPages = new HashSet<>();

            for (ModelPage page : pages) {
                ModelIndex index = indexRepository.findByLemmaAndPage(lemma, page);
                if (index != null) {
                    tempPages.add(page);
                }
            }

            pages = tempPages;
            if (pages.isEmpty()) {
                break;
            }
        }

        return new ArrayList<>(pages);
    }

    private Map<ModelPage, Float> calculateRelevance(List<ModelPage> pages, List<ModelLemma> lemmas) {
        Map<ModelPage, Float> relevanceMap = new HashMap<>();
        float maxAbsRelevance = 0;

        for (ModelPage page : pages) {
            float absRelevance = 0;
            for (ModelLemma lemma : lemmas) {
                ModelIndex index = indexRepository.findByLemmaAndPage(lemma, page);
                if (index != null) {
                    absRelevance += index.getRank();
                }
            }
            relevanceMap.put(page, absRelevance);
            if (absRelevance > maxAbsRelevance) {
                maxAbsRelevance = absRelevance;
            }
        }

        if (maxAbsRelevance > 0) {
            for (Map.Entry<ModelPage, Float> entry : relevanceMap.entrySet()) {
                entry.setValue(entry.getValue() / maxAbsRelevance);
            }
        }

        return relevanceMap;
    }

    private String createSnippet(String content, String[] queryStems) {
        String cleanText = stemmer.cleanText(content);

        if (cleanText.length() > 200) {
            cleanText = cleanText.substring(0, 200) + "...";
        }

        for (String stem : queryStems) {
            cleanText = cleanText.replaceAll("(?i)" + stem, "<b>" + stem + "</b>");
        }

        return cleanText;
    }

}
