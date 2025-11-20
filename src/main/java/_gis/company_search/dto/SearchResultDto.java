package _gis.company_search.dto;

import _gis.company_search.entity.SearchResultEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class SearchResultDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String city;
    private String query;
    private LocalDateTime createdAt;
    private GisSearchResponse results;
    private Integer resultsCount;

    public SearchResultDto() {}

    public SearchResultDto(SearchResultEntity entity, ObjectMapper mapper) {
        this.id = entity.getId();
        this.userId = entity.getUser().getId();
        this.userName = entity.getUser().getName();
        this.userEmail = entity.getUser().getEmail();
        this.city = entity.getCity();
        this.query = entity.getQuery();
        this.createdAt = entity.getCreatedAt();
        this.resultsCount = entity.getResultsCount();

        try {
            this.results = mapper.readValue(entity.getResultsJson(), GisSearchResponse.class);
        } catch (Exception e) {
            this.results = new GisSearchResponse();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public GisSearchResponse getResults() {
        return results;
    }

    public void setResults(GisSearchResponse results) {
        this.results = results;
    }

    public Integer getResultsCount() {
        return resultsCount;
    }

    public void setResultsCount(Integer resultsCount) {
        this.resultsCount = resultsCount;
    }
}
