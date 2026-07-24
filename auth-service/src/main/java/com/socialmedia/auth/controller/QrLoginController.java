package com.socialmedia.auth.controller;

import com.socialmedia.auth.dto.request.QrApproveRequest;
import com.socialmedia.auth.dto.request.QrInitRequest;
import com.socialmedia.auth.dto.response.MessageResponse;
import com.socialmedia.auth.dto.response.QrInitResponse;
import com.socialmedia.auth.dto.response.QrStatusResponse;
import com.socialmedia.auth.security.ClientIpResolver;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.QrLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/qr")
@Tag(name = "QR Login", description = "WhatsApp/Telegram-style QR code login for web/desktop clients")
public class QrLoginController {

    private final QrLoginService qrLoginService;
    private final CurrentUserProvider currentUserProvider;

    public QrLoginController(QrLoginService qrLoginService, CurrentUserProvider currentUserProvider) {
        this.qrLoginService = qrLoginService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/init")
    @Operation(summary = "Generate a QR code for an unauthenticated client to log in by scanning with the mobile app")
    public QrInitResponse init(@Valid @RequestBody QrInitRequest request) {
        return qrLoginService.init(request);
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve a QR login (called by the already-authenticated device that scanned it)")
    public MessageResponse approve(@Valid @RequestBody QrApproveRequest request) {
        qrLoginService.approve(currentUserProvider.get().getId(), currentUserProvider.principal().getDeviceId(),
                request.qrToken());
        return new MessageResponse("QR login approved");
    }

    @PostMapping("/deny")
    @Operation(summary = "Deny a QR login")
    public MessageResponse deny(@Valid @RequestBody QrApproveRequest request) {
        qrLoginService.deny(request.qrToken());
        return new MessageResponse("QR login denied");
    }

    @GetMapping("/status/{qrToken}")
    @Operation(summary = "Poll QR login status; returns tokens once approved (single-use)")
    public QrStatusResponse status(@PathVariable String qrToken, HttpServletRequest httpRequest) {
        return qrLoginService.poll(qrToken, ClientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
    }
}
