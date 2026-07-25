package com.socialmedia.media.repository;

import com.socialmedia.media.domain.MediaAsset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
}
