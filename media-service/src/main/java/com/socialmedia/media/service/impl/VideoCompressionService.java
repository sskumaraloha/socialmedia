package com.socialmedia.media.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Best-effort video transcoding by shelling out to ffmpeg, since no pure-Java library
 * does real video compression. If ffmpeg isn't on PATH (or the transcode fails/times
 * out), this returns empty rather than throwing - the original upload still gets stored
 * and served, just without a compressed variant. Not verifiable in a sandbox without
 * ffmpeg installed; the orchestration is real, the binary dependency is not guaranteed.
 */
@Service
public class VideoCompressionService {

    private static final Logger log = LoggerFactory.getLogger(VideoCompressionService.class);

    private final boolean enabled;
    private final long timeoutSeconds;

    public VideoCompressionService(@Value("${app.media.video-compression.enabled:true}") boolean enabled,
            @Value("${app.media.video-compression.timeout-seconds:120}") long timeoutSeconds) {
        this.enabled = enabled;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Optional<byte[]> compress(byte[] original, String fileExtension) {
        if (!enabled || !ffmpegAvailable()) {
            return Optional.empty();
        }

        Path inputFile = null;
        Path outputFile = null;
        try {
            inputFile = Files.createTempFile("media-in-", "." + fileExtension);
            outputFile = Files.createTempFile("media-out-", ".mp4");
            Files.write(inputFile, original);

            Process process = new ProcessBuilder("ffmpeg", "-y", "-i", inputFile.toString(),
                    "-vcodec", "libx264", "-crf", "28", "-preset", "veryfast", outputFile.toString())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffmpeg compression timed out after {}s", timeoutSeconds);
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                log.warn("ffmpeg compression exited with code {}", process.exitValue());
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(outputFile));
        } catch (IOException e) {
            log.warn("Video compression failed: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Video compression interrupted: {}", e.getMessage());
            return Optional.empty();
        } finally {
            deleteQuietly(inputFile);
            deleteQuietly(outputFile);
        }
    }

    private boolean ffmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            return process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // best-effort temp-file cleanup
            }
        }
    }
}
