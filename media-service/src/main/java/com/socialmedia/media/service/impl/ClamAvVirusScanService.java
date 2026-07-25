package com.socialmedia.media.service.impl;

import com.socialmedia.media.domain.VirusScanStatus;
import com.socialmedia.media.service.VirusScanService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Speaks clamd's INSTREAM protocol directly over a plain socket (no client library
 * needed): send "zINSTREAM\0", then the content as (4-byte big-endian length + chunk)
 * pairs terminated by a zero-length chunk, then read clamd's single reply line. If clamd
 * itself isn't reachable, this reports SCAN_UNAVAILABLE rather than throwing -
 * MediaProcessingService decides whether that fails the upload open or closed.
 */
@Service
public class ClamAvVirusScanService implements VirusScanService {

    private static final Logger log = LoggerFactory.getLogger(ClamAvVirusScanService.class);
    private static final int CHUNK_SIZE = 8192;

    private final String host;
    private final int port;
    private final int timeoutMillis;

    public ClamAvVirusScanService(@Value("${app.media.virus-scan.host:localhost}") String host,
            @Value("${app.media.virus-scan.port:3310}") int port,
            @Value("${app.media.virus-scan.timeout-ms:5000}") int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public VirusScanStatus scan(byte[] content) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            OutputStream out = socket.getOutputStream();
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int offset = 0; offset < content.length; offset += CHUNK_SIZE) {
                int length = Math.min(CHUNK_SIZE, content.length - offset);
                out.write(toBigEndian(length));
                out.write(content, offset, length);
            }
            out.write(toBigEndian(0));
            out.flush();

            String response = readResponse(socket.getInputStream());
            return interpret(response);
        } catch (IOException e) {
            log.warn("Virus scan unavailable (clamd unreachable at {}:{}): {}", host, port, e.getMessage());
            return VirusScanStatus.SCAN_UNAVAILABLE;
        }
    }

    private String readResponse(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim();
    }

    private VirusScanStatus interpret(String response) {
        if (response.contains("FOUND")) {
            log.warn("ClamAV flagged uploaded content: {}", response);
            return VirusScanStatus.INFECTED;
        }
        if (response.contains("OK")) {
            return VirusScanStatus.CLEAN;
        }
        log.warn("Unexpected ClamAV response: {}", response);
        return VirusScanStatus.SCAN_UNAVAILABLE;
    }

    private byte[] toBigEndian(int value) {
        return new byte[] {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }
}
