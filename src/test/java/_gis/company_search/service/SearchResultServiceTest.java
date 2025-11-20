package _gis.company_search.service;

import _gis.company_search.dto.GisSearchResponse;
import _gis.company_search.entity.SearchResultEntity;
import _gis.company_search.entity.UserEntity;
import _gis.company_search.repository.SearchResultRepository;
import _gis.company_search.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SearchResultServiceTest {
    private SearchResultRepository searchResultRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private SearchResultService searchResultService;

    private UserEntity testUser;
    private SearchResultEntity testEntity;
    private GisSearchResponse testResponse;

    @BeforeEach
    void setUp() throws Exception {
        searchResultRepository = mock(SearchResultRepository.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();

        searchResultService = new SearchResultService(
                searchResultRepository,
                userRepository,
                objectMapper
        );

        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");

        testResponse = new GisSearchResponse();
        testResponse.setItems(List.of());

        testEntity = new SearchResultEntity(
                testUser,
                "Москва",
                "кафе",
                objectMapper.writeValueAsString(testResponse),
                0
        );
        testEntity.setId(100L);
    }

    @Test
    void saveSearchResult_shouldSaveEntity() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(searchResultRepository.save(any(SearchResultEntity.class))).thenReturn(testEntity);

        StepVerifier.create(searchResultService.saveSearchResult(1L, "Москва", "кафе", testResponse))
                .expectNextMatches(entity -> entity.getId().equals(100L))
                .verifyComplete();

        verify(userRepository, times(1)).findById(1L);
        verify(searchResultRepository, times(1)).save(any(SearchResultEntity.class));
    }

    @Test
    void getUserSearchHistory_shouldReturnDtoList() {
        when(searchResultRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testEntity));

        StepVerifier.create(searchResultService.getUserSearchHistory(1L))
                .expectNextMatches(list -> list.size() == 1 &&
                        list.get(0).getId().equals(100L) &&
                        list.get(0).getUserName().equals("John Doe"))
                .verifyComplete();

        verify(searchResultRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getUserSearchHistoryWithFilters_shouldReturnFilteredDtoList() {
        when(searchResultRepository.findByUserIdWithFilters(1L, "Москва", "кафе"))
                .thenReturn(List.of(testEntity));

        StepVerifier.create(searchResultService.getUserSearchHistoryWithFilters(1L, "Москва", "кафе"))
                .expectNextMatches(list -> list.size() == 1 &&
                        list.get(0).getCity().equals("Москва") &&
                        list.get(0).getQuery().equals("кафе"))
                .verifyComplete();

        verify(searchResultRepository, times(1)).findByUserIdWithFilters(1L, "Москва", "кафе");
    }

    @Test
    void getSearchResultById_shouldReturnDto() {
        when(searchResultRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testEntity));

        StepVerifier.create(searchResultService.getSearchResultById(100L))
                .expectNextMatches(dto -> dto.getId().equals(100L) &&
                        dto.getUserId().equals(1L) &&
                        dto.getUserName().equals("John Doe"))
                .verifyComplete();

        verify(searchResultRepository, times(1)).findByIdWithUser(100L);
    }

    @Test
    void getSearchResultById_notFound_shouldThrow() {
        when(searchResultRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        StepVerifier.create(searchResultService.getSearchResultById(999L))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Search result not found"))
                .verify();

        verify(searchResultRepository, times(1)).findByIdWithUser(999L);
    }
}
