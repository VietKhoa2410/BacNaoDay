package demo.bacnaoday.service;

import demo.bacnaoday.api.payload.RelationPageResponse;
import demo.bacnaoday.model.RelationPage;
import demo.bacnaoday.repository.RelationPageRepository;
import demo.bacnaoday.repository.UserRepository;
import demo.bacnaoday.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class RelationPageService {

    private final RelationPageRepository relationPageRepository;
    private final UserRepository userRepository;

    public RelationPageService(RelationPageRepository relationPageRepository, UserRepository userRepository) {
        this.relationPageRepository = relationPageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RelationPageResponse> listForOwner(AppUserDetails user) {
        return relationPageRepository.findByOwner_IdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RelationPageResponse create(AppUserDetails user, String name) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is too long");
        }
        if (relationPageRepository.existsByOwner_IdAndName(user.getUserId(), trimmed)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A page with this name already exists");
        }
        RelationPage page = new RelationPage();
        page.setOwner(userRepository.getReferenceById(user.getUserId()));
        page.setName(trimmed);
        Instant now = Instant.now();
        page.setCreatedAt(now);
        page.setUpdatedAt(now);
        return toResponse(relationPageRepository.save(page));
    }

    @Transactional(readOnly = true)
    public RelationPageResponse getOwnedOrNotFound(AppUserDetails user, Long pageId) {
        return relationPageRepository
                .findByIdAndOwner_Id(pageId, user.getUserId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Resolves a page only if it belongs to the user; used for person operations.
     * Participates in the caller's transaction when one exists.
     */
    public RelationPage requireOwnedPage(AppUserDetails user, Long pageId) {
        return relationPageRepository
                .findByIdAndOwner_Id(pageId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private RelationPageResponse toResponse(RelationPage page) {
        return new RelationPageResponse(page.getId(), page.getName(), page.getCreatedAt());
    }
}
