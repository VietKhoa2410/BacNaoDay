package demo.bacnaoday.api;

import demo.bacnaoday.api.payload.RelativeLevelResponse;
import demo.bacnaoday.model.Person;
import demo.bacnaoday.repository.PersonRepository;
import demo.bacnaoday.security.AppUserDetails;
import demo.bacnaoday.service.PersonRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonRelativeLookupController {

    private final PersonRepository personRepository;
    private final PersonRelationService personRelationService;

    @GetMapping("/{personId}/relative-level")
    @Transactional(readOnly = true)
    public RelativeLevelResponse relativeLevel(
            @AuthenticationPrincipal AppUserDetails user, @PathVariable Long personId) {
        Person target =
                personRepository
                        .findByIdAndRelationPage_Owner_Id(personId, user.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var page = target.getRelationPage();
        if (page.getMarkedPerson() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no marked person on this relation page");
        }

        int level =
                personRelationService.computeRelativeLevel(
                        page.getId(), page.getMarkedPerson().getId(), target.getId());
        return new RelativeLevelResponse(level);
    }
}

