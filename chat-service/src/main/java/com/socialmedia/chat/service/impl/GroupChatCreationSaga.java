package com.socialmedia.chat.service.impl;

import com.socialmedia.chat.client.UserExistenceClient;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.exception.UserServiceUnavailableException;
import com.socialmedia.common.exception.BusinessException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Orchestration-style saga for group/channel creation, spanning chat-service (owns the chat
 * itself) and user-service (owns whether an invited member id is a real user). Deliberately
 * NOT @Transactional at this level - it is the orchestrator, not a step; each step it calls
 * on {@link GroupChatCreationSagaSteps} commits independently, and the external validation
 * call in between happens with no open database transaction.
 *
 * <p>Members are validated AFTER the chat is optimistically created rather than before, so
 * that the common case (every invited id is valid) never pays for N sequential synchronous
 * HTTP round trips on the critical path of every group chat creation - only the rare case of
 * a stale/invalid member id pays the (still cheap) cost of a compensating delete.
 */
@Component
public class GroupChatCreationSaga {

    private static final Logger log = LoggerFactory.getLogger(GroupChatCreationSaga.class);

    private final GroupChatCreationSagaSteps steps;
    private final UserExistenceClient userExistenceClient;

    public GroupChatCreationSaga(GroupChatCreationSagaSteps steps, UserExistenceClient userExistenceClient) {
        this.steps = steps;
        this.userExistenceClient = userExistenceClient;
    }

    public UUID execute(UUID requesterId, CreateGroupChatRequest request, String bearerAuthorizationHeader) {
        GroupChatCreationSagaSteps.StepResult created = steps.createLocally(requesterId, request);

        for (UUID memberId : created.invitedMemberIds()) {
            try {
                if (!userExistenceClient.exists(memberId, bearerAuthorizationHeader)) {
                    log.info("Compensating group chat {} creation - invited user {} does not exist", created.chatId(), memberId);
                    steps.compensate(created.chatId());
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CHAT_MEMBER",
                            "Invited user " + memberId + " does not exist");
                }
            } catch (UserServiceUnavailableException e) {
                log.warn("Compensating group chat {} creation - user-service unavailable while validating {}",
                        created.chatId(), memberId);
                steps.compensate(created.chatId());
                throw e;
            }
        }

        steps.confirm(created.chatId(), requesterId, created.allMemberIds());
        return created.chatId();
    }
}
