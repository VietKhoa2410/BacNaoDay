package demo.bacnaoday.api;

import demo.bacnaoday.api.payload.CreateRelationPageRequest;
import demo.bacnaoday.api.payload.MarkedPersonRequest;
import demo.bacnaoday.api.payload.RelationPageResponse;
import demo.bacnaoday.security.AppUserDetails;
import demo.bacnaoday.service.RelationPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/relation-pages")
@RequiredArgsConstructor
public class RelationPageController {

    private final RelationPageService relationPageService;

    @GetMapping
    public List<RelationPageResponse> list(@AuthenticationPrincipal AppUserDetails user) {
        return relationPageService.listForOwner(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelationPageResponse create(
            @AuthenticationPrincipal AppUserDetails user, @RequestBody CreateRelationPageRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }
        return relationPageService.create(user, body.name());
    }

    @GetMapping("/{id}")
    public RelationPageResponse getOne(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return relationPageService.getOwnedOrNotFound(user, id);
    }

    @PutMapping("/{id}/marked-person")
    public RelationPageResponse setMarkedPerson(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long id,
            @RequestBody MarkedPersonRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }
        return relationPageService.setMarkedPerson(user, id, body.personId());
    }
}
