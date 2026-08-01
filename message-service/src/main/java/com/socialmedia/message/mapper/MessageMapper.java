package com.socialmedia.message.mapper;

import com.socialmedia.message.domain.Draft;
import com.socialmedia.message.domain.EncryptedEnvelope;
import com.socialmedia.message.domain.MediaMetadata;
import com.socialmedia.message.domain.Message;
import com.socialmedia.message.dto.response.DraftResponse;
import com.socialmedia.message.dto.response.MediaResponse;
import com.socialmedia.message.dto.response.MessageResponse;
import com.socialmedia.message.dto.response.ReactionResponse;
import com.socialmedia.message.dto.response.ReceiptResponse;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    /** Projects the message for exactly one requesting device: only that device's ciphertext
     * envelope is included, since it is the only one that device could decrypt anyway. */
    public MessageResponse toResponse(Message message, String viewerDeviceId) {
        MediaResponse media = message.getMedia() == null ? null : toMediaResponse(message.getMedia());
        EncryptedEnvelope envelope = viewerDeviceId == null ? null : message.envelopeFor(viewerDeviceId).orElse(null);
        return new MessageResponse(
                message.getId(),
                message.getChatId(),
                message.getSenderId(),
                message.getType(),
                envelope == null ? null : envelope.getCipherType(),
                envelope == null ? null : envelope.getCiphertext(),
                media,
                message.getReplyToMessageId(),
                message.getForwardedFromMessageId(),
                message.getReactions().stream()
                        .map(r -> new ReactionResponse(r.getUserId(), r.getEmoji(), r.getReactedAt()))
                        .toList(),
                message.getReceipts().stream()
                        .map(r -> new ReceiptResponse(r.getUserId(), r.getDeliveredAt(), r.getReadAt()))
                        .toList(),
                message.isEdited(),
                message.getEditedAt(),
                message.isDeletedForEveryone(),
                message.getScheduledAt(),
                message.isSent(),
                message.getSelfDestructAt(),
                message.getCreatedAt(),
                message.getUpdatedAt());
    }

    public MediaResponse toMediaResponse(MediaMetadata media) {
        return new MediaResponse(media.getUrl(), media.getMimeType(), media.getSizeBytes(), media.getDurationSeconds());
    }

    public DraftResponse toDraftResponse(Draft draft) {
        return new DraftResponse(draft.getChatId(), draft.getContent(), draft.getUpdatedAt());
    }
}
