package com.socialmedia.user.controller;

import com.socialmedia.common.security.CurrentUser;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blocks")
@Tag(name = "Blocking", description = "Block/unblock other users")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping
    @Operation(summary = "List blocked users")
    public List<UserSummaryResponse> list() {
        return blockService.listBlocked(CurrentUser.get().userId());
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block a user")
    public void block(@PathVariable UUID userId) {
        blockService.block(CurrentUser.get().userId(), userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unblock a user")
    public void unblock(@PathVariable UUID userId) {
        blockService.unblock(CurrentUser.get().userId(), userId);
    }
}
