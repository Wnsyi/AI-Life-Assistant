package com.lifeassistant.config;

import com.lifeassistant.model.Holiday;
import com.lifeassistant.model.Quote;
import com.lifeassistant.repository.HolidayRepository;
import com.lifeassistant.repository.QuoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据库初始化 — 节假日 + 名言名句
 *
 * 每条数据存在 MySQL 里，Agent 通过工具查询，不再硬编码
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Override
    public void run(String... args) {
        initHolidays();
        initQuotes();
    }

    // ==================== 节假日 ====================

    private void initHolidays() {
        if (holidayRepository.count() > 0) {
            System.out.println("[DataInit] 节假日数据已存在，跳过");
            return;
        }
        System.out.println("[DataInit] 正在插入节假日...");

        holidayRepository.saveAll(List.of(
            hd("2026-01-01", "元旦"), hd("2026-01-02", "元旦假期"), hd("2026-01-03", "元旦假期"),
            hd("2026-02-15", "春节假期"), hd("2026-02-16", "春节假期"), hd("2026-02-17", "除夕"),
            hd("2026-02-18", "春节"), hd("2026-02-19", "春节假期"), hd("2026-02-20", "春节假期"),
            hd("2026-02-21", "春节假期"), hd("2026-02-22", "春节假期"), hd("2026-02-23", "春节假期"),
            wk("2026-02-14", "春节前补班"), wk("2026-02-28", "春节后补班"),
            hd("2026-04-04", "清明节假期"), hd("2026-04-05", "清明节"), hd("2026-04-06", "清明节假期"),
            hd("2026-05-01", "劳动节"), hd("2026-05-02", "劳动节假期"), hd("2026-05-03", "劳动节假期"),
            hd("2026-05-04", "劳动节假期"), hd("2026-05-05", "劳动节假期"), wk("2026-05-09", "劳动节补班"),
            hd("2026-06-19", "端午节"), hd("2026-06-20", "端午节假期"), hd("2026-06-21", "端午节假期"),
            wk("2026-06-22", "端午节补班"),
            hd("2026-09-25", "中秋节"), hd("2026-09-26", "中秋节假期"), hd("2026-09-27", "中秋节假期"),
            hd("2026-10-01", "国庆节"), hd("2026-10-02", "国庆节假期"), hd("2026-10-03", "国庆节假期"),
            hd("2026-10-04", "国庆节假期"), hd("2026-10-05", "国庆节假期"), hd("2026-10-06", "国庆节假期"),
            hd("2026-10-07", "国庆节假期"), wk("2026-09-20", "中秋前补班"),
            wk("2026-10-10", "国庆后补班"), wk("2026-10-11", "国庆后补班")
        ));
        System.out.println("[DataInit] ✅ 节假日 " + holidayRepository.count() + " 条");
    }

    // ==================== 名言名句 ====================

    private void initQuotes() {
        if (quoteRepository.count() > 0) {
            System.out.println("[DataInit] 名言数据已存在，跳过");
            return;
        }
        System.out.println("[DataInit] 正在插入名言...");

        quoteRepository.saveAll(List.of(
            q("千里之行，始于足下", "老子", "哲理"),
            q("学而不思则罔，思而不学则殆", "孔子", "哲理"),
            q("天行健，君子以自强不息", "《周易》", "励志"),
            q("路漫漫其修远兮，吾将上下而求索", "屈原", "励志"),
            q("宝剑锋从磨砺出，梅花香自苦寒来", "《警世贤文》", "励志"),
            q("不积跬步，无以至千里；不积小流，无以成江海", "荀子", "哲理"),
            q("三人行，必有我师焉", "孔子", "哲理"),
            q("己所不欲，勿施于人", "孔子", "哲理"),
            q("人生自古谁无死，留取丹心照汗青", "文天祥", "励志"),
            q("山重水复疑无路，柳暗花明又一村", "陆游", "哲理"),
            q("书山有路勤为径，学海无涯苦作舟", "韩愈", "励志"),
            q("先天下之忧而忧，后天下之乐而乐", "范仲淹", "励志"),
            q("海内存知己，天涯若比邻", "王勃", "生活"),
            q("读书破万卷，下笔如有神", "杜甫", "哲理"),
            q("纸上得来终觉浅，绝知此事要躬行", "陆游", "哲理"),
            q("少壮不努力，老大徒伤悲", "《长歌行》", "励志"),
            q("天生我材必有用，千金散尽还复来", "李白", "励志"),
            q("会当凌绝顶，一览众山小", "杜甫", "励志"),
            q("莫愁前路无知己，天下谁人不识君", "高适", "励志"),
            q("落红不是无情物，化作春泥更护花", "龚自珍", "哲理"),
            q("春蚕到死丝方尽，蜡炬成灰泪始干", "李商隐", "哲理"),
            q("沉舟侧畔千帆过，病树前头万木春", "刘禹锡", "哲理"),
            q("千磨万击还坚劲，任尔东西南北风", "郑燮", "励志"),
            q("大鹏一日同风起，扶摇直上九万里", "李白", "励志"),
            q("欲穷千里目，更上一层楼", "王之涣", "哲理"),
            q("采菊东篱下，悠然见南山", "陶渊明", "生活"),
            q("言必信，行必果", "孔子", "哲理"),
            q("老骥伏枥，志在千里；烈士暮年，壮心不已", "曹操", "励志"),
            q("勿以恶小而为之，勿以善小而不为", "刘备", "哲理"),
            q("读万卷书，行万里路", "董其昌", "哲理")
        ));
        System.out.println("[DataInit] ✅ 名言 " + quoteRepository.count() + " 条");
    }

    // ==================== 工厂方法 ====================

    private Holiday hd(String date, String name) {
        Holiday h = new Holiday();
        h.setYear(Integer.parseInt(date.substring(0, 4)));
        h.setHolidayDate(LocalDate.parse(date));
        h.setHolidayName(name);
        h.setIsHoliday(true);
        h.setIsWorkday(false);
        return h;
    }

    private Holiday wk(String date, String name) {
        Holiday h = new Holiday();
        h.setYear(Integer.parseInt(date.substring(0, 4)));
        h.setHolidayDate(LocalDate.parse(date));
        h.setHolidayName(name);
        h.setIsHoliday(false);
        h.setIsWorkday(true);
        return h;
    }

    private Quote q(String content, String author, String category) {
        Quote q = new Quote();
        q.setContent(content);
        q.setAuthor(author);
        q.setCategory(category);
        return q;
    }
}
