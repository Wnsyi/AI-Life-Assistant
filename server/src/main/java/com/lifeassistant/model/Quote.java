package com.lifeassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quotes")
@Data
@NoArgsConstructor
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 名言内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 作者/出处 */
    @Column(nullable = false, length = 100)
    private String author;

    /** 分类: 励志/哲理/诗词/生活 */
    @Column(length = 50)
    private String category = "通用";
}
