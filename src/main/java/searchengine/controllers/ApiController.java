package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.SearchStatistics;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexPageService;
import searchengine.services.SearchService;
import searchengine.services.WebParseService;
import searchengine.services.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SitesList sitesList;
    private final SearchService searchService;
    private final WebParseService parseService;
    private final IndexPageService indexPageService;

    public ApiController(StatisticsService statisticsService, SitesList sitesList, SearchService searchService, WebParseService parseService, IndexPageService indexPageService) {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
        this.searchService = searchService;
        this.parseService = parseService;
        this.indexPageService = indexPageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        try {
            parseService.startIndexing();
            return ResponseEntity.ok("{\"result\": true}");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body("{\"result\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        try {
            parseService.stopIndexing();
            return ResponseEntity.ok("{\"result\": true}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"result\": false, \"error\": \"Ошибка при остановке индексации\"}");
        }
    }

    @GetMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        try {
            boolean result = indexPageService.indexPage(url);

            if (result) {
                return ResponseEntity.ok("{\"result\": true}");
            } else {
                return ResponseEntity.badRequest()
                        .body("{\"result\": false, \"error\": \"Данная страница находится за пределами сайтов, указанных в конфигурационном файле\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"result\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("{\"result\": false, \"error\": \"Задан пустой поисковый запрос\"}");
            }

            List<SearchStatistics> results = searchService.search(query, site, offset, limit);

            SearchResponse response = new SearchResponse();
            response.setResult(true);
            response.setCount(results.size());
            response.setData(results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"result\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

}
