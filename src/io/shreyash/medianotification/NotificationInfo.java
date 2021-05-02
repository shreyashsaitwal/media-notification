package io.shreyash.medianotification;

import android.support.v4.media.MediaMetadataCompat;

public class NotificationInfo {
  private final int priority;
  private boolean isPlaying;
  private MediaMetadataCompat metadata;

  public NotificationInfo(int priority, boolean isPlaying, MediaMetadataCompat metadata) {
    this.priority = priority;
    this.isPlaying = isPlaying;
    this.metadata = metadata;
  }

  public void setIsPlaying(boolean playing) {
    isPlaying = playing;
  }

  public void setMetadata(MediaMetadataCompat metadata) {
    this.metadata = metadata;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isPlaying() {
    return isPlaying;
  }

  public MediaMetadataCompat getMetadata() {
    return metadata;
  }
}
