package demo.bacnaoday.api;

import demo.bacnaoday.api.payload.CreatePersonRequest;
import demo.bacnaoday.api.payload.PersonGraphResponse;
import demo.bacnaoday.api.payload.PersonResponse;
import demo.bacnaoday.security.AppUserDetails;
import demo.bacnaoday.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/relation-pages/{pageId}/persons")
@RequiredArgsConstructor
public class RelationPagePersonController {

    private final PersonService personService;

    @GetMapping
    public List<PersonResponse> list(
            @AuthenticationPrincipal AppUserDetails user, @PathVariable Long pageId) {
        return personService.listForPage(user, pageId);
    }

    @GetMapping("/graph")
    public PersonGraphResponse graph(
            @AuthenticationPrincipal AppUserDetails user, @PathVariable Long pageId) {
        return personService.graphForPage(user, pageId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse create(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long pageId,
            @RequestBody CreatePersonRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }
        return personService.create(user, pageId, body);
    }
}
