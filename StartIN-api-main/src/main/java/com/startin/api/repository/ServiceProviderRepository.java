package com.startin.api.repository;

import com.startin.api.model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

    Optional<ServiceProvider> findByUserId(Long userId);

    List<ServiceProvider> findAllByOrderByMatchScoreDesc();

    List<ServiceProvider> findByCategoryIgnoreCase(String category);

    List<ServiceProvider> findByCategoryIgnoreCaseOrderByMatchScoreDesc(String category);

    @Query("SELECT p FROM ServiceProvider p WHERE " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.mainService) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.toolsAndSkills) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ServiceProvider> searchProviders(@Param("category") String category, @Param("query") String query);

    default List<ServiceProvider> searchProviders(String query) {
        return searchProviders(null, query);
    }
}
