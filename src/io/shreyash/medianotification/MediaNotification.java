package io.shreyash.medianotification;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MediaNotification extends AndroidNonvisibleComponent {
  private final Context context;
  private final Activity activity;
  private final Form form;

  private final MediaSessionCompat mediaSession;
  private final NotificationManagerCompat notificationManager;

  private String channelId = "MediaNotificationChannelID";
  private String channelName = "MediaNotificationChannel";
  private int channelImp = NotificationManager.IMPORTANCE_DEFAULT;

  private final Map<Integer, NotificationInfo> notificationInfo = new HashMap<>();

  public MediaNotification(ComponentContainer container) {
    super(container.$form());
    this.form = container.$form();
    this.activity = form.$context();
    this.context = activity.getApplicationContext();

    createChannel();
    this.mediaSession = new MediaSessionCompat(context, "MediaNotificationExtSession");
    this.notificationManager = NotificationManagerCompat.from(context);

    registerActionReceiver(activity);
  }

  private void registerActionReceiver(Activity activity) {
    final BroadcastReceiver mediaActionReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final Bundle extras = intent.getExtras();
        final int id = extras.getInt("id");

        if (intent.getAction().equals(ActionType.PLAY)) {
          final NotificationInfo info = notificationInfo.get(id);
          final boolean isPlaying = info.isPlaying();

          ShowNotification(id, info.getPriority(), info.getMetadata());
          ActionButtonClicked(isPlaying ? ActionType.PAUSE : ActionType.PLAY, id);

          info.setIsPlaying(!isPlaying);
        } else {
          ActionButtonClicked(intent.getAction(), id);
        }
      }
    };

    activity.registerReceiver(mediaActionReceiver, new IntentFilter(ActionType.PLAY));
    activity.registerReceiver(mediaActionReceiver, new IntentFilter(ActionType.NEXT));
    activity.registerReceiver(mediaActionReceiver, new IntentFilter(ActionType.PREV));
  }

  private void createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      final NotificationChannel channel = new NotificationChannel(channelId, channelName, channelImp);
      final NotificationManager manager = activity.getSystemService(NotificationManager.class);
      manager.createNotificationChannel(channel);
    }
  }

  private NotificationCompat.Action buildAction(int notificationId, String actionType) {
    final Intent intent = new Intent(actionType);
    intent.putExtra("id", notificationId);

    final PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
    );

    final int icon;
    if (ActionType.PLAY.equals(actionType)) {
      if (notificationInfo.get(notificationId).isPlaying()) {
        icon = android.R.drawable.ic_media_pause;
      } else {
        icon = android.R.drawable.ic_media_play;
      }
    } else if (ActionType.NEXT.equals(actionType)) {
      icon = android.R.drawable.ic_media_next;
    } else {
      icon = android.R.drawable.ic_media_previous;
    }

    return new NotificationCompat.Action.Builder(icon, actionType, pendingIntent).build();
  }

  @SimpleFunction()
  public void ShowNotification(int id, int priority, Object metadata) {
    mediaSession.setMetadata((MediaMetadataCompat) metadata);
    MediaMetadataCompat metadataCompat = mediaSession.getController().getMetadata();

    if (notificationInfo.containsKey(id)) {
      final NotificationInfo info = notificationInfo.get(id);
      info.setIsPlaying(!info.isPlaying());
    } else {
      notificationInfo.put(id, new NotificationInfo(priority, true, metadataCompat));
    }

    final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(mediaSession.getSessionToken())
        )
        .setPriority(priority)
        .setSmallIcon(activity.getApplicationInfo().icon)
        .setContentTitle(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        .setContentText(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
        .setLargeIcon(metadataCompat.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
        .addAction(buildAction(id, ActionType.PREV))
        .addAction(buildAction(id, ActionType.PLAY))
        .addAction(buildAction(id, ActionType.NEXT));

    notificationManager.notify(id, builder.build());
  }

  @SimpleFunction
  public Object CreateMetadata(String title, String artist, String albumArt) {
    final MediaMetadataCompat metadata;
    try {
      metadata = new MediaMetadataCompat.Builder()
          .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
          .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
          .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeStream(form.openAsset(albumArt)))
          .build();
    } catch (IOException e) {
      e.printStackTrace();
      throw new YailRuntimeError("Failed to open asset '" + albumArt + "'.\n" + e.getMessage(), "IOException");
    }

    return metadata;
  }

  @SimpleFunction
  public void CancelNotification(int id) {
    if (notificationInfo.containsKey(id)) {
      notificationManager.cancel(id);
      notificationInfo.remove(id);
    } else {
      throw new YailRuntimeError("Cannot remove non-existent notification ID: " + id, "Invalid ID");
    }
  }

  @SimpleFunction
  public void CancelAllNotifications() {
    if (!notificationInfo.isEmpty()) {
      notificationManager.cancelAll();
      notificationInfo.clear();
    }
  }

  @SimpleEvent
  public void ActionButtonClicked(String actionType, int notificationId) {
    EventDispatcher.dispatchEvent(this, "ActionButtonClicked", actionType, notificationId);
  }

  @SimpleProperty
  public int PriorityHigh() {
    return NotificationCompat.PRIORITY_HIGH;
  }

  @SimpleProperty
  public int PriorityLow() {
    return NotificationCompat.PRIORITY_LOW;
  }

  @SimpleProperty
  public int PriorityMax() {
    return NotificationCompat.PRIORITY_MAX;
  }

  @SimpleProperty
  public int PriorityMin() {
    return NotificationCompat.PRIORITY_MIN;
  }

  @SimpleProperty
  public int PriorityDefault() {
    return NotificationCompat.PRIORITY_DEFAULT;
  }

  // Channel ID
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "MediaNotificationChannelID"
  )
  @SimpleProperty(userVisible = false)
  public void ChannelID(String id) {
    this.channelId = id;
  }

  @SimpleProperty
  public String ChannelID() {
    return this.channelId;
  }

  // Channel name
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "MediaNotificationChannel"
  )
  @SimpleProperty(userVisible = false)
  public void ChannelName(String name) {
    this.channelName = name;
  }

  @SimpleProperty
  public String ChannelName() {
    return this.channelName;
  }

  // Channel importance
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
      defaultValue = "Default",
      editorArgs = {
          "Default", "High", "Low", "Max", "Min", "None", "Unspecified"
      },
      alwaysSend = true
  )
  @SimpleProperty(userVisible = false)
  public void ChannelImportance(String importance) {
    Log.i(importance, "LOG MN");
    switch (importance) {
      case "Default":
        this.channelImp = NotificationManager.IMPORTANCE_DEFAULT;
        break;
      case "None":
        this.channelImp = NotificationManager.IMPORTANCE_NONE;
        break;
      case "Unspecified":
        this.channelImp = NotificationManager.IMPORTANCE_UNSPECIFIED;
        break;
      case "High":
        this.channelImp = NotificationManager.IMPORTANCE_HIGH;
        break;
      case "Low":
        this.channelImp = NotificationManager.IMPORTANCE_LOW;
        break;
      case "Max":
        this.channelImp = NotificationManager.IMPORTANCE_MAX;
        break;
      case "Min":
        this.channelImp = NotificationManager.IMPORTANCE_MIN;
        break;
    }
  }

  @SimpleProperty
  public String ChannelImportance() {
    switch (this.channelImp) {
      case NotificationManager.IMPORTANCE_DEFAULT:
        return "Default";
      case NotificationManager.IMPORTANCE_NONE:
        return "None";
      case NotificationManager.IMPORTANCE_UNSPECIFIED:
        return "Unspecified";
      case NotificationManager.IMPORTANCE_HIGH:
        return "High";
      case NotificationManager.IMPORTANCE_LOW:
        return "Low";
      case NotificationManager.IMPORTANCE_MAX:
        return "Max";
      case NotificationManager.IMPORTANCE_MIN:
        return "Min";
      default:
        return null;
    }
  }
}
