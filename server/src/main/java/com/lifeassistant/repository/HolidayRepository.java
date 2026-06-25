package com.lifeassistant.repository;

import com.lifeassistant.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /** 查指定日期是否是节假日 */
    Optional<Holiday> findByHolidayDate(LocalDate date);

    /** 查某年所有节假日 */
    List<Holiday> findByYearAndIsHolidayTrueOrderByHolidayDate(Integer year);

    /** 查某年所有调休上班日 */
    List<Holiday> findByYearAndIsWorkdayTrueOrderByHolidayDate(Integer year);
}
