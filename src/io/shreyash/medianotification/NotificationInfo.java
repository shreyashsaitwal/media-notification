package io.shreyash.medianotification;

import android.support.v4.media.MediaMetadataCompat;

public class NotificationInfo {
  private final int priority;
  private final boolean onlyAlertOnce;
  private boolean isPlaying;
  private MediaMetadataCompat metadata;

  public NotificationInfo(int priority, boolean onlyAlertOnce, boolean isPlaying, MediaMetadataCompat metadata) {
    this.priority = priority;
    this.onlyAlertOnce = onlyAlertOnce;
    this.isPlaying = isPlaying;
    this.metadata = metadata;
  }

  public void setIsPlaying(boolean playing) {
    isPlaying = playing;
  }

  public int getPriority() {
    return priority;
  }

  public boolean getOnlyAlertOnce() {
    return onlyAlertOnce;
  }

  public boolean getIsPlaying() {
    return isPlaying;
  }

  public MediaMetadataCompat getMetadata() {
    return metadata;
  }
}
