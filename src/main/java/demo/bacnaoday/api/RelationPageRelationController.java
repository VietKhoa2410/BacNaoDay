package demo.bacnaoday.api;

import demo.bacnaoday.security.AppUserDetails;
import demo.bacnaoday.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relation-page/{pageId}")
@RequiredArgsConstructor
public class RelationPageRelationController {

    private final PersonService personService;

    @DeleteMapping("/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long pageId,
            @PathVariable Long personId) {
        personService.delete(user, pageId, personId);
    }
}
