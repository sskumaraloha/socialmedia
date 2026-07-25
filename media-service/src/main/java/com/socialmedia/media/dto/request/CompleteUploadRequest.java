package com.socialmedia.media.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompleteUploadRequest(@NotEmpty List<CompletedPartRequest> parts) {
}
