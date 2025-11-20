package _gis.company_search.repository;

import _gis.company_search.entity.SearchResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchResultRepository extends JpaRepository<SearchResultEntity, Long>{
    @Query("SELECT s FROM SearchResultEntity s JOIN FETCH s.user WHERE s.user.id = :userId")
    List<SearchResultEntity> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM SearchResultEntity s JOIN FETCH s.user WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<SearchResultEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT s FROM SearchResultEntity s LEFT JOIN FETCH s.user WHERE s.id = :id")
    Optional<SearchResultEntity> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT s FROM SearchResultEntity s JOIN FETCH s.user WHERE s.user.id = :userId " +
            "AND (:city IS NULL OR s.city = :city) " +
            "AND (:query IS NULL OR s.query LIKE %:query%) " +
            "ORDER BY s.createdAt DESC")
    List<SearchResultEntity> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("city") String city,
            @Param("query") String query
    );
}
