package _gis.company_search.service;

import _gis.company_search.dto.GisSearchResponse;
import _gis.company_search.dto.SearchResultDto;
import _gis.company_search.entity.SearchResultEntity;
import _gis.company_search.entity.UserEntity;
import _gis.company_search.repository.SearchResultRepository;
import _gis.company_search.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchResultService {
    private final SearchResultRepository searchResultRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SearchResultService(
            SearchResultRepository searchResultRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.searchResultRepository = searchResultRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Mono<SearchResultEntity> saveSearchResult(
            Long userId,
            String city,
            String query,
            GisSearchResponse response) {

        return Mono.fromCallable(() -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            String resultsJson = objectMapper.writeValueAsString(response);

            SearchResultEntity entity = new SearchResultEntity(
                    user,
                    city,
                    query,
                    resultsJson,
                    response.getItems().size()
            );

            return searchResultRepository.save(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public Mono<List<SearchResultDto>> getUserSearchHistory(Long userId) {
        return Mono.fromCallable(() -> {
            List<SearchResultEntity> entities = searchResultRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return entities.stream()
                    .map(entity -> new SearchResultDto(entity, objectMapper))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public Mono<List<SearchResultDto>> getUserSearchHistoryWithFilters(
            Long userId,
            String city,
            String query) {
        return Mono.fromCallable(() -> {
            List<SearchResultEntity> entities = searchResultRepository.findByUserIdWithFilters(userId, city, query);
            return entities.stream()
                    .map(entity -> new SearchResultDto(entity, objectMapper))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public Mono<SearchResultDto> getSearchResultById(Long id) {
        return Mono.fromCallable(() -> {
            SearchResultEntity entity = searchResultRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new RuntimeException("Search result not found with id: " + id));
            return new SearchResultDto(entity, objectMapper);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public List<SearchResultDto> getUserSearchHistoryParallel(Long userId, String city, String query) {
        List<SearchResultEntity> results = searchResultRepository.findByUserId(userId);

        return results.parallelStream()
                .filter(result -> city == null || result.getCity().equals(city))
                .filter(result -> query == null || result.getQuery().contains(query))
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .map(entity -> new SearchResultDto(entity, objectMapper))
                .toList();
    }
}
