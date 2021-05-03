package io.shreyash.medianotification;

import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;

public class NotificationInfo {
  private final int priority;
  private final MediaMetadataCompat metadata;
  private boolean isPlaying;
  private Bitmap albumArtCache = null;

  public NotificationInfo(int priority, boolean isPlaying, MediaMetadataCompat metadata) {
    this.priority = priority;
    this.isPlaying = isPlaying;
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    NotificationInfo info = (NotificationInfo) object;

    final String albumKey = MediaMetadataCompat.METADATA_KEY_ALBUM_ART;
    final String albumUriKey = MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI;
    final String titleKey = MediaMetadataCompat.METADATA_KEY_TITLE;
    final String artistKey = MediaMetadataCompat.METADATA_KEY_ARTIST;

    final boolean result = priority == info.priority &&
        metadata.getString(titleKey).equals(info.metadata.getString(titleKey)) &&
        metadata.getString(artistKey).equals(info.metadata.getString(artistKey));

    if (metadata.getBitmap(albumKey) != null && info.metadata.getBitmap(albumKey) != null) {
      return result &&
          (metadata.getBitmap(albumKey).equals(info.metadata.getBitmap(albumKey))
              || metadata.getString(albumUriKey).equals(info.metadata.getString(albumUriKey)));
    } else {
      return result &&
          metadata.getString(albumUriKey).equals(info.metadata.getString(albumUriKey));
    }
  }

  public int getPriority() {
    return priority;
  }

  public boolean getIsPlaying() {
    return isPlaying;
  }

  public void setIsPlaying(boolean playing) {
    isPlaying = playing;
  }

  public MediaMetadataCompat getMetadata() {
    return metadata;
  }

  public Bitmap getAlbumArtCache() {
    return albumArtCache;
  }

  public void setAlbumArtCache(Bitmap albumArtCache) {
    this.albumArtCache = albumArtCache;
  }
}
