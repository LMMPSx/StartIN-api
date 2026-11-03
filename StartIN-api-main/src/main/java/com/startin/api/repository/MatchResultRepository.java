package com.startin.api.repository;

import com.startin.api.model.MatchResult;
import com.startin.api.model.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByStartupProfileIdOrderByMatchScoreDesc(Long startupId);

    Optional<MatchResult> findByStartupProfileIdAndOpportunityId(Long startupId, Long opportunityId);

    List<MatchResult> findByStartupProfileIdAndStatus(Long startupId, MatchStatus status);

    List<MatchResult> findByOpportunityCompanyIdOrderByMatchScoreDesc(Long companyId);

    long countByStartupProfileIdAndStatus(Long startupId, MatchStatus status);
}
