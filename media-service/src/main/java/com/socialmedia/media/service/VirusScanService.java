package com.socialmedia.media.service;

import com.socialmedia.media.domain.VirusScanStatus;

public interface VirusScanService {

    VirusScanStatus scan(byte[] content);
}
