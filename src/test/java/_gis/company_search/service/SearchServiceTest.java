package _gis.company_search.service;

import _gis.company_search.dto.GisItem;
import _gis.company_search.dto.GisSearchResponse;
import _gis.company_search.entity.SearchResultEntity;
import _gis.company_search.exception.DoubleGisApiException;
import _gis.company_search.logging.AsyncLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private MockWebServer mockWebServer;
    private SearchService searchService;
    private WebClient webClient;
    private ObjectMapper objectMapper;
    private ExecutorService executorService;

    @Mock
    private AsyncLogger asyncLogger;

    @Mock
    private SearchResultService searchResultService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        executorService = Executors.newFixedThreadPool(2);
        webClient = WebClient.builder().build();

        String baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");

        searchService = new SearchService(
                "test-api-key",
                baseUrl,
                webClient,
                executorService,
                objectMapper,
                asyncLogger,
                searchResultService
        );
    }

    @AfterEach
    void tearDown() throws IOException, InterruptedException {
        mockWebServer.shutdown();
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void search_SuccessfulResponse_ReturnsGisSearchResponse() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": [
                            {
                                "id": "123",
                                "name": "Тестовое кафе",
                                "address_name": "ул. Тестовая, 1"
                            },
                            {
                                "id": "456",
                                "name": "Другое кафе",
                                "address_name": "пр. Тестовый, 2"
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getItems()).hasSize(2);
                    assertThat(response.getItems().get(0).getId()).isEqualTo("123");
                    assertThat(response.getItems().get(0).getName()).isEqualTo("Тестовое кафе");
                    assertThat(response.getItems().get(0).getAddress()).isEqualTo("ул. Тестовая, 1");
                    assertThat(response.getItems().get(1).getId()).isEqualTo("456");
                    assertThat(response.getItems().get(1).getName()).isEqualTo("Другое кафе");
                })
                .verifyComplete();

        verify(searchResultService, times(1))
                .saveSearchResult(eq(1L), eq("Москва"), eq("кафе"), any(GisSearchResponse.class));
    }

    @Test
    void search_EmptyResults_ReturnsEmptyList() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": []
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "несуществующее"))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getItems()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void search_ApiReturns404_ThrowsDoubleGisApiException() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 404,
                        "error": {
                            "message": "Results not found",
                            "type": "itemNotFound"
                        }
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectError(DoubleGisApiException.class)
                .verify();

        verify(searchResultService, never()).saveSearchResult(anyLong(), anyString(), anyString(), any());
        verify(asyncLogger, atLeastOnce()).error(contains("HTTP ошибка"), eq(null));
    }

    @Test
    void search_ApiReturns500_ThrowsDoubleGisApiException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectError(DoubleGisApiException.class)
                .verify();

        verify(searchResultService, never()).saveSearchResult(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void search_InvalidJsonResponse_ThrowsRuntimeException() {
        String invalidJson = "{ invalid json }";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(invalidJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectError(RuntimeException.class)
                .verify();

        verify(searchResultService, never()).saveSearchResult(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void search_SaveResultFails_PropagatesError() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": [
                            {
                                "id": "123",
                                "name": "Тестовое кафе",
                                "address_name": "ул. Тестовая, 1"
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectErrorMessage("Database error")
                .verify();
    }

    @Test
    void search_SpecialCharactersInQuery_EncodesCorrectly() throws InterruptedException {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": []
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе & ресторан"))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getItems()).isEmpty();
                })
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("%26");
    }

    @Test
    void search_CyrillicCharacters_EncodesCorrectly() throws InterruptedException {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": []
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("%D0%9C");
    }

    @Test
    void search_MultipleItems_ParsesAllCorrectly() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": [
                            {
                                "id": "1",
                                "name": "Кафе 1",
                                "address_name": "Адрес 1"
                            },
                            {
                                "id": "2",
                                "name": "Кафе 2",
                                "address_name": "Адрес 2"
                            },
                            {
                                "id": "3",
                                "name": "Кафе 3",
                                "address_name": "Адрес 3"
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .assertNext(response -> {
                    assertThat(response.getItems()).hasSize(3);
                    assertThat(response.getItems())
                            .extracting(GisItem::getName)
                            .containsExactly("Кафе 1", "Кафе 2", "Кафе 3");
                    assertThat(response.getItems())
                            .extracting(GisItem::getId)
                            .containsExactly("1", "2", "3");
                    assertThat(response.getItems())
                            .extracting(GisItem::getAddress)
                            .containsExactly("Адрес 1", "Адрес 2", "Адрес 3");
                })
                .verifyComplete();
    }

    @Test
    void search_LogsCorrectMessages() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": [
                            {
                                "id": "123",
                                "name": "Тестовое кафе",
                                "address_name": "ул. Тестовая, 1"
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectNextCount(1)
                .verifyComplete();

        verify(asyncLogger, atLeastOnce()).info(contains("Начало поиска"));
        verify(asyncLogger, atLeastOnce()).info(contains("Сформирован URL запроса"));
        verify(asyncLogger, atLeastOnce()).info(contains("Длина тела ответа"));
        verify(asyncLogger, atLeastOnce()).info(contains("Тело ответа"));
        verify(asyncLogger, atLeastOnce()).info(contains("Парсинг завершен успешно"));
        verify(asyncLogger, atLeastOnce()).info(contains("Сохранение результатов поиска"));
        verify(asyncLogger, atLeastOnce()).info(contains("Результаты успешно сохранены"));
    }

    @Test
    void search_ApiKeyIncludedInRequest() throws InterruptedException {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": []
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("key=test-api-key");
    }

    @Test
    void search_RequestParametersCorrect() throws InterruptedException {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": []
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath())
                .contains("type=branch")
                .contains("page_size=10")
                .contains("page=1");
    }

    @Test
    void search_HandlesNullOrEmptyFields() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": [
                            {
                                "id": "",
                                "name": "",
                                "address_name": ""
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        SearchResultEntity mockEntity = new SearchResultEntity();
        mockEntity.setId(1L);
        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(mockEntity));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .assertNext(response -> {
                    assertThat(response.getItems()).hasSize(1);
                    assertThat(response.getItems().get(0).getId()).isEmpty();
                    assertThat(response.getItems().get(0).getName()).isEmpty();
                    assertThat(response.getItems().get(0).getAddress()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void search_ErrorLogging_WhenSaveFails() {
        String mockResponse = """
                {
                    "meta": {
                        "api_version": "3.0",
                        "code": 200
                    },
                    "result": {
                        "items": []
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        when(searchResultService.saveSearchResult(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        StepVerifier.create(searchService.search(1L, "Москва", "кафе"))
                .expectError(RuntimeException.class)
                .verify();

        verify(asyncLogger, atLeastOnce()).error(contains("Ошибка сохранения результатов в БД"), any());
        verify(asyncLogger, atLeastOnce()).error(contains("Ошибка выполнения поиска"), any());
    }
}