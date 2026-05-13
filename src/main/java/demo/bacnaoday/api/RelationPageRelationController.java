package demo.bacnaoday.api;

import demo.bacnaoday.api.payload.MarkedPersonRequest;
import demo.bacnaoday.api.payload.RelationPageResponse;
import demo.bacnaoday.security.AppUserDetails;
import demo.bacnaoday.service.PersonService;
import demo.bacnaoday.service.RelationPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/relation-page/{pageId}")
@RequiredArgsConstructor
public class RelationPageRelationController {

    private final PersonService personService;
    private final RelationPageService relationPageService;

    @PutMapping("/marked-person")
    public RelationPageResponse setMarkedPerson(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long pageId,
            @RequestBody MarkedPersonRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }
        return relationPageService.setMarkedPerson(user, pageId, body.personId());
    }

    @DeleteMapping("/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long pageId,
            @PathVariable Long personId) {
        personService.delete(user, pageId, personId);
    }
}
