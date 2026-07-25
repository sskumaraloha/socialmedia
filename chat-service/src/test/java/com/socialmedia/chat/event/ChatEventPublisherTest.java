package com.socialmedia.chat.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.domain.ChatType;
import com.socialmedia.chat.outbox.OutboxEvent;
import com.socialmedia.chat.outbox.OutboxEventRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatEventPublisherTest {

    @Mock private OutboxEventRepository outboxEventRepository;

    private ChatEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ChatEventPublisher(new ObjectMapper().findAndRegisterModules(), outboxEventRepository);
    }

    @Test
    void publishChatCreatedStagesAnOutboxRowInsteadOfCallingKafkaDirectly() {
        UUID chatId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Chat chat = new Chat(ChatType.GROUP, "Team", null, creatorId, false);
        chat.setId(chatId);

        publisher.publishChatCreated(chat, Set.of(creatorId));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent staged = captor.getValue();
        assertThat(staged.getTopic()).isEqualTo(ChatTopics.CHAT_CREATED);
        assertThat(staged.getAggregateKey()).isEqualTo(chatId.toString());
        assertThat(staged.getEventType()).isEqualTo("ChatCreatedEvent");
        assertThat(staged.getPayload()).contains(chatId.toString()).contains("Team");
        assertThat(staged.isPublished()).isFalse();
    }

    @Test
    void publishChatDeletedStagesTheDeletionEvent() {
        UUID chatId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();

        publisher.publishChatDeleted(chatId, deletedBy);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo(ChatTopics.CHAT_DELETED);
        assertThat(captor.getValue().getPayload()).contains(deletedBy.toString());
    }
}
