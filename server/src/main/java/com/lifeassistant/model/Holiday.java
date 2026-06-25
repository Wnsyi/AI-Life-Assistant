package com.lifeassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * 节假日表
 */
@Entity
@Table(name = "holidays")
@Data
@NoArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 年份 */
    @Column(nullable = false)
    private Integer year;

    /** 日期 */
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    /** 节日名称 */
    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    /** 是否放假 */
    @Column(nullable = false)
    private Boolean isHoliday = true;

    /** 是否是调休上班日 */
    @Column(name = "is_workday")
    private Boolean isWorkday = false;
}
