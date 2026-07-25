package com.socialmedia.user.service.impl;

import com.socialmedia.common.exception.ResourceNotFoundException;
import com.socialmedia.user.domain.Contact;
import com.socialmedia.user.domain.UserProfile;
import com.socialmedia.user.dto.request.AddContactRequest;
import com.socialmedia.user.dto.response.ContactResponse;
import com.socialmedia.user.exception.CannotActOnSelfException;
import com.socialmedia.user.mapper.UserProfileMapper;
import com.socialmedia.user.repository.ContactRepository;
import com.socialmedia.user.repository.UserProfileRepository;
import com.socialmedia.user.service.ContactService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper mapper;

    public ContactServiceImpl(ContactRepository contactRepository, UserProfileRepository userProfileRepository,
            UserProfileMapper mapper) {
        this.contactRepository = contactRepository;
        this.userProfileRepository = userProfileRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ContactResponse addContact(UUID ownerId, AddContactRequest request) {
        if (ownerId.equals(request.contactUserId())) {
            throw new CannotActOnSelfException("add");
        }
        UserProfile contactProfile = userProfileRepository.findById(request.contactUserId())
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", request.contactUserId()));

        Contact contact = contactRepository.findByOwnerIdAndContactUserId(ownerId, request.contactUserId())
                .orElseGet(() -> contactRepository.save(new Contact(ownerId, request.contactUserId(), request.nickname())));
        if (request.nickname() != null && !request.nickname().equals(contact.getNickname())) {
            contact.setNickname(request.nickname());
            contactRepository.save(contact);
        }

        return new ContactResponse(contactProfile.getId(), contact.getNickname(), mapper.toSummary(contactProfile),
                contact.getCreatedAt());
    }

    @Override
    @Transactional
    public void removeContact(UUID ownerId, UUID contactUserId) {
        contactRepository.deleteByOwnerIdAndContactUserId(ownerId, contactUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> listContacts(UUID ownerId) {
        List<Contact> contacts = contactRepository.findByOwnerId(ownerId);
        Map<UUID, UserProfile> profilesById = userProfileRepository
                .findByIdIn(contacts.stream().map(Contact::getContactUserId).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return contacts.stream()
                .filter(c -> profilesById.containsKey(c.getContactUserId()))
                .map(c -> new ContactResponse(c.getContactUserId(), c.getNickname(),
                        mapper.toSummary(profilesById.get(c.getContactUserId())), c.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
