package com.startin.api.repository;

import com.startin.api.model.Opportunity;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    Optional<Opportunity> findByCode(String code);

    List<Opportunity> findByType(OpportunityType type);

    List<Opportunity> findByStatus(OpportunityStatus status);

    List<Opportunity> findByCompanyIdOrderByIdDesc(Long companyId);

    List<Opportunity> findByStartupIdOrderByIdDesc(Long startupId);

    @Query("SELECT o FROM Opportunity o WHERE " +
           "LOWER(o.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.shortDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.tags) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Opportunity> search(@Param("query") String query);

    @Query("SELECT o FROM Opportunity o WHERE o.type = :type AND (" +
           "LOWER(o.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.shortDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Opportunity> searchByType(@Param("type") OpportunityType type, @Param("query") String query);
}
