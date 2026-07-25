package com.socialmedia.chat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.chat.client.UserExistenceClient;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.exception.UserServiceUnavailableException;
import com.socialmedia.common.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupChatCreationSagaTest {

    @Mock private GroupChatCreationSagaSteps steps;
    @Mock private UserExistenceClient userExistenceClient;

    private GroupChatCreationSaga saga;

    @BeforeEach
    void setUp() {
        saga = new GroupChatCreationSaga(steps, userExistenceClient);
    }

    @Test
    void confirmsTheChatWhenEveryInvitedMemberExists() {
        UUID requesterId = UUID.randomUUID();
        UUID memberA = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        CreateGroupChatRequest request = new CreateGroupChatRequest("Team", null, Set.of(memberA), false, false);
        Set<UUID> invited = new LinkedHashSet<>(Set.of(memberA));
        Set<UUID> all = new LinkedHashSet<>(Set.of(memberA, requesterId));
        when(steps.createLocally(requesterId, request)).thenReturn(new GroupChatCreationSagaSteps.StepResult(chatId, invited, all));
        when(userExistenceClient.exists(memberA, "Bearer token")).thenReturn(true);

        UUID result = saga.execute(requesterId, request, "Bearer token");

        assertThat(result).isEqualTo(chatId);
        verify(steps).confirm(chatId, requesterId, all);
        verify(steps, never()).compensate(any());
    }

    @Test
    void compensatesAndThrowsWhenAnInvitedMemberDoesNotExist() {
        UUID requesterId = UUID.randomUUID();
        UUID ghostMember = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        CreateGroupChatRequest request = new CreateGroupChatRequest("Team", null, Set.of(ghostMember), false, false);
        Set<UUID> invited = new LinkedHashSet<>(Set.of(ghostMember));
        Set<UUID> all = new LinkedHashSet<>(Set.of(ghostMember, requesterId));
        when(steps.createLocally(requesterId, request)).thenReturn(new GroupChatCreationSagaSteps.StepResult(chatId, invited, all));
        when(userExistenceClient.exists(ghostMember, "Bearer token")).thenReturn(false);

        assertThatThrownBy(() -> saga.execute(requesterId, request, "Bearer token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ghostMember.toString());

        verify(steps).compensate(chatId);
        verify(steps, never()).confirm(any(), any(), any());
    }

    @Test
    void compensatesAndRethrowsWhenUserServiceIsUnreachable() {
        UUID requesterId = UUID.randomUUID();
        UUID memberA = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        CreateGroupChatRequest request = new CreateGroupChatRequest("Team", null, Set.of(memberA), false, false);
        Set<UUID> invited = new LinkedHashSet<>(Set.of(memberA));
        Set<UUID> all = new LinkedHashSet<>(Set.of(memberA, requesterId));
        when(steps.createLocally(requesterId, request)).thenReturn(new GroupChatCreationSagaSteps.StepResult(chatId, invited, all));
        when(userExistenceClient.exists(eq(memberA), any())).thenThrow(new UserServiceUnavailableException());

        assertThatThrownBy(() -> saga.execute(requesterId, request, "Bearer token"))
                .isInstanceOf(UserServiceUnavailableException.class);

        verify(steps).compensate(chatId);
        verify(steps, never()).confirm(any(), any(), any());
    }
}
