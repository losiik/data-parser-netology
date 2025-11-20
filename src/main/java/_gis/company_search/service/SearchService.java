package _gis.company_search.service;

import _gis.company_search.dto.GisItem;
import _gis.company_search.dto.GisSearchResponse;
import _gis.company_search.exception.DoubleGisApiException;
import _gis.company_search.logging.AsyncLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

@Service
public class SearchService {
    private final String apiUrl;
    private final String apiKey;
    private final WebClient dgClient;
    private final ObjectMapper mapper;
    private final Scheduler customScheduler;
    private final AsyncLogger logger;
    private final SearchResultService searchResultService;

    public SearchService(
            @Value("${dgis.api.key}") String apiKey,
            @Value("${dgis.base.api.url}") String apiUrl,
            WebClient dgClient,
            ExecutorService searchExecutorService,
            ObjectMapper mapper,
            AsyncLogger logger,
            SearchResultService searchResultService
    ) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.dgClient = dgClient;
        this.mapper = mapper;
        this.customScheduler = Schedulers.fromExecutorService(searchExecutorService);
        this.logger = logger;
        this.searchResultService = searchResultService;
    }

    public Mono<GisSearchResponse> search(Long userId, String city, String name) {
        logger.info("Начало поиска для города: " + city + ", запрос: " + name);

        return Mono.fromCallable(() -> {
                    logger.debug("Подготовка запроса в потоке: " + Thread.currentThread().getName());
            return URLEncoder.encode(city + " " + name, StandardCharsets.UTF_8);
        })
        .subscribeOn(customScheduler)
        .flatMap(query -> {
            String fullUrl = apiUrl + "/items?q=" + query +
                    "&type=branch&page_size=10&page=1&key=" + apiKey;

            logger.info("Сформирован URL запроса: " + fullUrl);
            logger.debug("Отправка HTTP запроса к 2GIS API");
            return dgClient.get()
                    .uri(uriBuilder -> {
                        URI uri = URI.create(fullUrl);
                        return uri;
                    })
                    .retrieve()
                    .onStatus(
                            status -> status.value() != 200,
                            response -> {
                                logger.error("HTTP ошибка: " + response.statusCode().value(), null);
                                return Mono.error(new DoubleGisApiException());
                            }
                    )
                    .bodyToMono(String.class)
                    .doOnNext(body -> {
                        logger.info("Длина тела ответа: " + body.length() + " символов");
                        logger.info("Тело ответа: " + body);
                    });
        })
        .map(responseBody -> {
            try {
                logger.debug("Парсинг ответа в потоке: " + Thread.currentThread().getName());

                List<GisItem> gisItems = new ArrayList<>();
                JsonNode root = mapper.readTree(responseBody);

                logger.debug("JSON распарсен. Структура корня: " + root.toString());

                JsonNode items = root.path("result").path("items");

                for (JsonNode item : items) {
                    String itemName = item.path("name").asText();
                    logger.info(itemName);
                    String itemValue = item.path("address_name").asText();
                    String itemId = item.path("id").asText();
                    GisItem gisItem = new GisItem(itemId, itemName, itemValue);
                    gisItems.add(gisItem);
                }

                logger.info("Парсинг завершен успешно. Найдено элементов: " + gisItems.size());
                return new GisSearchResponse(gisItems);
            } catch (Exception e) {
                logger.error("Ошибка парсинга JSON", e);
                throw new RuntimeException("Ошибка парсинга JSON", e);
            }
        })
        .flatMap(response -> {
            logger.info("Сохранение результатов поиска в БД для пользователя: " + userId);
            return searchResultService.saveSearchResult(userId, city, name, response)
                    .doOnSuccess(saved -> logger.info("Результаты успешно сохранены с ID: " + saved.getId()))
                    .doOnError(error -> logger.error("Ошибка сохранения результатов в БД", error))
                    .thenReturn(response);
        })
        .doOnError(error -> logger.error("Ошибка выполнения поиска: " + error.getMessage(), error));
    }
}
