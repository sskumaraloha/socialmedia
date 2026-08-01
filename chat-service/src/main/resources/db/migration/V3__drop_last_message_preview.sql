-- Messages became end-to-end encrypted, so message-service can no longer publish a plaintext
-- content preview and this column can never be populated again. The chat list's last-message
-- preview is now rendered client-side from the locally-decrypted message, as it is in WhatsApp
-- and Signal. Dropping the column rather than leaving it permanently NULL keeps the schema
-- honest about what the server can actually know.

ALTER TABLE chats DROP COLUMN last_message_preview;
