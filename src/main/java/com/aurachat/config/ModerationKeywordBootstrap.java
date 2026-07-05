package com.aurachat.config;

import com.aurachat.module.moderation.entity.ModerationKeyword;
import com.aurachat.module.moderation.repository.ModerationKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationKeywordBootstrap implements CommandLineRunner {

    private static final List<String> DEFAULT_KEYWORDS = List.of(
        "lừa đảo", "scam", "lừa tiền", "ma túy", "khiêu dâm", "sex", "porn", "xxx", "nude",
        "đm", "dm", "vcl", "vl", "clgt", "đụ", "địt", "cặc", "lồn", "chó", "đĩ", "điếm",
        "giết người", "khủng bố", "phản động", "bạo lực", "tự tử", "spam", "quảng cáo rác"
    );

    private final ModerationKeywordRepository keywordRepository;

    @Override
    public void run(String... args) {
        if (keywordRepository.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        DEFAULT_KEYWORDS.forEach(word -> keywordRepository.save(ModerationKeyword.builder()
            .word(word)
            .enabled(true)
            .createdAt(now)
            .build()));
        log.info("Moderation keyword bootstrap seeded {} keywords", DEFAULT_KEYWORDS.size());
    }
}
