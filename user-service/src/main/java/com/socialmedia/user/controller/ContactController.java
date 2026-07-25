package com.socialmedia.user.controller;

import com.socialmedia.common.security.CurrentUser;
import com.socialmedia.user.dto.request.AddContactRequest;
import com.socialmedia.user.dto.response.ContactResponse;
import com.socialmedia.user.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Address-book style contacts, distinct from follow/followers")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "List contacts")
    public List<ContactResponse> list() {
        return contactService.listContacts(CurrentUser.get().userId());
    }

    @PostMapping
    @Operation(summary = "Add a contact")
    public ContactResponse add(@Valid @RequestBody AddContactRequest request) {
        return contactService.addContact(CurrentUser.get().userId(), request);
    }

    @DeleteMapping("/{contactUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a contact")
    public void remove(@PathVariable UUID contactUserId) {
        contactService.removeContact(CurrentUser.get().userId(), contactUserId);
    }
}
