package _gis.company_search.controller;

import _gis.company_search.dto.GisSearchResponse;
import _gis.company_search.dto.SearchResultDto;
import _gis.company_search.logging.AsyncLogger;
import _gis.company_search.service.SearchResultService;
import org.springframework.web.bind.annotation.*;
import _gis.company_search.service.SearchService;
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;
    private final SearchResultService searchResultService;
    private final AsyncLogger logger;

    public SearchController(
            SearchService searchService,
            SearchResultService searchResultService,
            AsyncLogger logger) {
        this.searchService = searchService;
        this.searchResultService = searchResultService;
        this.logger = logger;
    }

    @GetMapping
    public Mono<GisSearchResponse> search(
            @RequestParam Long userId,
            @RequestParam String city,
            @RequestParam String text) {
        logger.info("Получен запрос на поиск. Город: " + city + ", текст: " + text +
                " в потоке: " + Thread.currentThread().getName());
        return searchService.search(userId, city, text);
    }

    @GetMapping("/history")
    public Mono<List<SearchResultDto>> getSearchHistory(@RequestParam Long userId) {
        logger.info("Получен запрос истории поиска для пользователя: " + userId);
        return searchResultService.getUserSearchHistory(userId);
    }

    @GetMapping("/history/filter")
    public Mono<List<SearchResultDto>> getSearchHistoryWithFilters(
            @RequestParam Long userId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String query) {
        logger.info("Получен запрос истории с фильтрами. Пользователь: " + userId +
                ", город: " + city + ", запрос: " + query);
        return searchResultService.getUserSearchHistoryWithFilters(userId, city, query);
    }

    @GetMapping("/history/{id}")
    public Mono<SearchResultDto> getSearchResultById(@PathVariable Long id) {
        logger.info("Получен запрос результата поиска по ID: " + id);
        return searchResultService.getSearchResultById(id);
    }
}
