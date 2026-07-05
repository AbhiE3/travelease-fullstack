package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId);

    List<SearchHistory> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT sh.source, sh.destination FROM SearchHistory sh " +
           "WHERE sh.userId = :userId " +
           "GROUP BY sh.source, sh.destination " +
           "ORDER BY MAX(sh.searchedAt) DESC")
    List<Object[]> findRecentlySearchedRoutes(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT sh.source, sh.destination, COUNT(sh) as searchCount FROM SearchHistory sh " +
           "GROUP BY sh.source, sh.destination " +
           "ORDER BY searchCount DESC")
    List<Object[]> findPopularRoutes(Pageable pageable);
}
