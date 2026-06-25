package com.lifeassistant.repository;

import com.lifeassistant.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    /** 随机取一条 */
    @Query(value = "SELECT * FROM quotes ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Quote findRandom();

    /** 按分类取 */
    Quote findFirstByCategoryOrderById(String category);
}
