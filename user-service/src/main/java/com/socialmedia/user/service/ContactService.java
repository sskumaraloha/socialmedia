package com.socialmedia.user.service;

import com.socialmedia.user.dto.request.AddContactRequest;
import com.socialmedia.user.dto.response.ContactResponse;
import java.util.List;
import java.util.UUID;

public interface ContactService {

    ContactResponse addContact(UUID ownerId, AddContactRequest request);

    void removeContact(UUID ownerId, UUID contactUserId);

    List<ContactResponse> listContacts(UUID ownerId);
}
