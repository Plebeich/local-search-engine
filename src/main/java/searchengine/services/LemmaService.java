package searchengine.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.ModelIndex;
import searchengine.model.ModelLemma;
import searchengine.model.ModelPage;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.stemmer.RussianStemmer;

import java.util.Map;

@Service
public class LemmaService {
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private RussianStemmer stemmer; //временное решение потому как нет библиотеки скилбокса

    @Transactional
    public void indexPage(ModelPage modelPage){
        try {
            System.out.println("начало лемматизации для страницы" + modelPage.getPath());
            String text = stemmer.cleanText(modelPage.getContent());
            Map<String, Integer> stems = stemmer.getStems(text);
            System.out.println("Найдено стемов: " + stems.size());

            for (Map.Entry<String, Integer> stem : stems.entrySet()) {
                String keyStem = stem.getKey();
                int valueStem = stem.getValue();
                if (keyStem.length() < 3) {
                    continue;
                }

                ModelLemma modelLemma = lemmaRepository.findByLemmaAndSite(keyStem, modelPage.getSite());
                if (modelLemma == null) {
                    modelLemma = new ModelLemma();
                    modelLemma.setLemma(keyStem);
                    modelLemma.setSite(modelPage.getSite());
                    modelLemma.setFrequency(1);
                    lemmaRepository.save(modelLemma);
                    System.out.println("новая лемма: " + keyStem);
                } else {
                    modelLemma.setFrequency(modelLemma.getFrequency() + 1);
                    lemmaRepository.save(modelLemma);
                }

                ModelIndex modelIndex = new ModelIndex();
                modelIndex.setPage(modelPage);
                modelIndex.setLemma(modelLemma);
                modelIndex.setRank((float) valueStem);
                indexRepository.save(modelIndex);
            }
            System.out.println("Индексация завершена для страницы: " + modelPage.getPath());
        }catch (Exception e){
            System.out.println("Ошибка при индексации " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void deletePageData(ModelPage modelPage) {
        indexRepository.deleteByPage(modelPage);
        System.out.println("Данные страницы удалены: " + modelPage.getPath());
    }

}
