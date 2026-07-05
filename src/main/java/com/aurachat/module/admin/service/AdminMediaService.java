package com.aurachat.module.admin.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.admin.dto.AdminMediaDto;
import com.aurachat.module.admin.dto.AdminMediaStatsResponse;
import com.aurachat.module.admin.dto.PageResponse;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.media.entity.Media;
import com.aurachat.module.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMediaService {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final AdminPostService adminPostService;

    public PageResponse<AdminMediaDto> getAllMedia(
        Pageable pageable,
        String queryText,
        String mediaType,
        String ownerId,
        Boolean includeDeleted
    ) {
        Query query = buildMediaQuery(queryText, mediaType, ownerId, includeDeleted);
        long total = mongoTemplate.count(query, Media.class);
        query.with(pageable);
        List<Media> mediaList = mongoTemplate.find(query, Media.class);
        Map<String, User> owners = loadOwners(mediaList);
        List<AdminMediaDto> content = mediaList.stream()
            .map(media -> AdminMediaDto.from(media, owners.get(media.getOwnerId())))
            .toList();
        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public AdminMediaDto getMediaById(String mediaId) {
        Media media = requireMedia(mediaId);
        User owner = userRepository.findById(media.getOwnerId()).orElse(null);
        return AdminMediaDto.from(media, owner);
    }

    public AdminMediaStatsResponse getStats() {
        long total = mongoTemplate.count(new Query(), Media.class);
        long active = mongoTemplate.count(Query.query(Criteria.where("deleted").is(false)), Media.class);
        long imageCount = mongoTemplate.count(activeTypeQuery("IMAGE"), Media.class);
        long fileCount = mongoTemplate.count(activeTypeQuery("FILE"), Media.class);
        long audioCount = mongoTemplate.count(activeTypeQuery("AUDIO"), Media.class);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("deleted").is(false)),
            Aggregation.group().sum("size").as("total")
        );
        AggregationResults<SizeSumResult> results = mongoTemplate.aggregate(
            aggregation, Media.class, SizeSumResult.class);
        SizeSumResult sum = results.getUniqueMappedResult();
        long totalBytes = sum == null ? 0 : sum.total();

        return new AdminMediaStatsResponse(
            total, active, total - active, totalBytes, imageCount, fileCount, audioCount
        );
    }

    public void deleteMedia(String mediaId, String adminId) {
        Media media = requireMedia(mediaId);
        mediaService.adminDeleteMedia(mediaId, adminId);
        adminPostService.deletePostsReferencingImageUrl(media.getUrl(), adminId);
    }

    private Query buildMediaQuery(String queryText, String mediaType, String ownerId, Boolean includeDeleted) {
        List<Criteria> filters = new ArrayList<>();
        if (includeDeleted == null || !includeDeleted) {
            filters.add(Criteria.where("deleted").is(false));
        }
        if (mediaType != null && !mediaType.isBlank()) {
            filters.add(Criteria.where("mediaType").is(mediaType.trim().toUpperCase()));
        }
        if (ownerId != null && !ownerId.isBlank()) {
            filters.add(Criteria.where("ownerId").is(ownerId.trim()));
        }
        if (queryText != null && !queryText.isBlank()) {
            String trimmed = queryText.trim();
            String regex = Pattern.quote(trimmed);
            List<String> ownerIds = findUserIdsBySearch(trimmed);
            List<Criteria> searchCriteria = new ArrayList<>(List.of(
                Criteria.where("fileName").regex(regex, "i"),
                Criteria.where("originalFileName").regex(regex, "i")
            ));
            ownerIds.forEach(id -> searchCriteria.add(Criteria.where("ownerId").is(id)));
            filters.add(new Criteria().orOperator(searchCriteria.toArray(Criteria[]::new)));
        }
        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
        }
        return query;
    }

    private Query activeTypeQuery(String mediaType) {
        return Query.query(Criteria.where("deleted").is(false).and("mediaType").is(mediaType));
    }

    private Map<String, User> loadOwners(List<Media> mediaList) {
        List<String> ownerIds = mediaList.stream().map(Media::getOwnerId).distinct().toList();
        if (ownerIds.isEmpty()) return Map.of();
        return userRepository.findAllById(ownerIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));
    }

    private Media requireMedia(String mediaId) {
        Media media = mongoTemplate.findById(mediaId, Media.class);
        if (media == null) {
            throw new BusinessLogicException(ErrorCode.MEDIA_NOT_FOUND, "Media not found");
        }
        return media;
    }

    private List<String> findUserIdsBySearch(String queryText) {
        String regex = Pattern.quote(queryText.trim());
        Query userQuery = Query.query(new Criteria().orOperator(
            Criteria.where("displayName").regex(regex, "i"),
            Criteria.where("email").regex(regex, "i")
        ));
        return mongoTemplate.find(userQuery, User.class).stream().map(User::getId).toList();
    }

    public record SizeSumResult(long total) {}
}
