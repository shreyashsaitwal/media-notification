package io.shreyash.medianotification;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
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
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MediaNotification extends AndroidNonvisibleComponent implements OnDestroyListener {
  private final Context context;
  private final Activity activity;
  private final Form form;

  private final MediaSessionCompat mediaSession;
  private final NotificationManagerCompat notificationManager;

  private String channelId = "MediaNotificationChannelID";
  private String channelName = "MediaNotificationChannel";
  private int channelImp = NotificationManager.IMPORTANCE_DEFAULT;

  private final Map<Integer, NotificationInfo> notificationInfo = new HashMap<>();
  private BroadcastReceiver mediaActionReceiver;

  public MediaNotification(ComponentContainer container) {
    super(container.$form());
    this.form = container.$form();
    this.activity = form.$context();
    this.context = activity.getApplicationContext();

    this.mediaSession = new MediaSessionCompat(context, "MediaNotificationExtSession");
    this.notificationManager = NotificationManagerCompat.from(context);

    registerActionReceiver(activity);
    form.registerForOnDestroy(this);
  }

  @Override
  public void onDestroy() {
    activity.unregisterReceiver(mediaActionReceiver);
  }

  /**
   * Registers the broadcast receivers responsible for handling the media
   * notification action's click events.
   *
   * @param activity the current activity
   */
  private void registerActionReceiver(Activity activity) {
    mediaActionReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final Bundle extras = intent.getExtras();
        final int id = extras.getInt("id");

        if (intent.getAction().equals(ActionType.PLAY)) {
          final NotificationInfo info = notificationInfo.get(id);
          final boolean isPlaying = info.getIsPlaying();

          ActionButtonClicked(isPlaying ? ActionType.PAUSE : ActionType.PLAY, id);
          info.setIsPlaying(!isPlaying);

          // Showing the notification again with the same ID updates it rather
          // than creating a new notification. Here, we are updating the extension
          // to change middle action's icon (play/pause).
          ShowNotification(id, info.getPriority(), info.getMetadata());
        } else {
          ActionButtonClicked(intent.getAction(), id);
        }
      }
    };

    // Register the receiver
    activity.registerReceiver(mediaActionReceiver, new IntentFilter(ActionType.PLAY));
    activity.registerReceiver(mediaActionReceiver, new IntentFilter(ActionType.NEXT));
    activity.registerReceiver(mediaActionReceiver, new IntentFilter(ActionType.PREV));
  }

  /**
   * Creates a notification channel on API 26 and above.
   */
  private void createChannel() {
    final NotificationManager manager = activity.getSystemService(NotificationManager.class);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
      final NotificationChannel channel = new NotificationChannel(channelId, channelName, channelImp);
      manager.createNotificationChannel(channel);
    }
  }

  /**
   * Builds an action button for the notification.
   *
   * @param notificationId ID of the notification for which action is to be created.
   * @param actionType     The type of this action; see {@link ActionType}
   */
  private NotificationCompat.Action buildAction(int notificationId, String actionType) {
    createChannel();

    final Intent intent = new Intent(actionType);
    intent.putExtra("id", notificationId);

    final PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
    );

    final int icon;
    if (ActionType.PLAY.equals(actionType)) {
      if (notificationInfo.get(notificationId).getIsPlaying()) {
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

  /**
   * Shows a media style notification.
   *
   * @param id       A unique integer that identifies this notification.
   * @param priority The priority of this notification.
   * @param metadata Metadata of the media being played.
   */
  @SimpleFunction(description = "Shows a media style notification.")
  public void ShowNotification(int id, int priority, Object metadata) {
    mediaSession.setMetadata((MediaMetadataCompat) metadata);
    MediaMetadataCompat metadataCompat = mediaSession.getController().getMetadata();

    NotificationInfo info = new NotificationInfo(priority, true, metadataCompat);
    if (!notificationInfo.containsKey(id) || !notificationInfo.get(id).equals(info)) {
      notificationInfo.put(id, info);
    }

    final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(mediaSession.getSessionToken())
        )
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(priority)
        .setSmallIcon(activity.getApplicationInfo().icon)
        .setContentTitle(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        .setContentText(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
        .addAction(buildAction(id, ActionType.PREV))
        .addAction(buildAction(id, ActionType.PLAY))
        .addAction(buildAction(id, ActionType.NEXT))
        .setOnlyAlertOnce(true);

    if (!metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI).equals("")) {
      final Bitmap cachedAlbumArt = notificationInfo.get(id).getAlbumArtCache();

      // Fetching a remote image could be expensive. Use the cached album art if this
      // notification is being updated.
      if (cachedAlbumArt != null) {
        builder.setLargeIcon(cachedAlbumArt);
        notificationManager.notify(id, builder.build());
      } else {
        // Fetching data over the internet blocks the UI thread. To keep Android happy,
        // we need to fetch the album art from a new thread.
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              final URL url = new URL(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
              final Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());

              notificationInfo.get(id).setAlbumArtCache(bitmap);

              builder.setLargeIcon(bitmap);
              notificationManager.notify(id, builder.build());
            } catch (MalformedURLException e) {
              e.printStackTrace();
              throw new YailRuntimeError(e.toString(), "MalformedURLException");
            } catch (IOException e) {
              e.printStackTrace();
              throw new YailRuntimeError(e.toString(), "IOException");
            }
          }
        }).start();
      }
    } else {
      builder.setLargeIcon(metadataCompat.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
      notificationManager.notify(id, builder.build());
    }
  }

  /**
   * Creates a metadata object from the given params and returns it.
   *
   * @param title    The title of the media.
   * @param artist   The artist for the album of the media's original source.
   * @param albumArt The artwork for the album of the media's original source.
   *                 It can be an URL or an image from assets.
   */
  @SimpleFunction(description = "Creates a metadata object from the given params and returns it.")
  public Object CreateMetadata(String title, String artist, String albumArt) {
    // RegEx pattern that matches all the valid URLs
    final Pattern urlPattern =
        Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()!@:%_+.~#?&//=]*)");

    final MediaMetadataCompat.Builder metadata;
    metadata = new MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);

    if (urlPattern.matcher(albumArt).find()) {
      metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArt);
    } else {
      try {
        metadata
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeStream(form.openAsset(albumArt)))
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, "");
      } catch (IOException e) {
        e.printStackTrace();
        throw new YailRuntimeError("Something went wrong while loading albumArt.\n" + e.toString(), "IOException");
      }
    }

    return metadata.build();
  }

  /**
   * Cancels the notification with the given ID.
   *
   * @param id The ID of the notification which is to be canceled.
   */
  @SimpleFunction(description = "Cancels the notification with given ID.")
  public void CancelNotification(int id) {
    if (notificationInfo.containsKey(id)) {
      notificationManager.cancel(id);
      notificationInfo.remove(id);
    } else {
      throw new YailRuntimeError("Cannot remove non-existent notification ID: " + id, "Invalid ID");
    }
  }

  /**
   * Cancels all the previously shown notifications.
   */
  @SimpleFunction(description = "Cancels all the previously shown notifications.")
  public void CancelAllNotifications() {
    if (!notificationInfo.isEmpty()) {
      notificationManager.cancelAll();
      notificationInfo.clear();
    }
  }

  /**
   * Fires when one of the action button on the notification is clicked.
   *
   * @param actionType     The type of action that was clicked; see {@link ActionType}
   * @param notificationId The ID of the notification who's action was clicked.
   */
  @SimpleEvent(description = "Fires when one of the action button on the notification is clicked.")
  public void ActionButtonClicked(String actionType, int notificationId) {
    EventDispatcher.dispatchEvent(this, "ActionButtonClicked", actionType, notificationId);
  }


  @SimpleProperty(
      description = "Higher notification priority, for more important notifications or alerts. The" +
          " UI may choose to show these items larger, or at a different position in notification" +
          " lists, compared with your app's PriorityDefault items."
  )
  public int PriorityHigh() {
    return NotificationCompat.PRIORITY_HIGH;
  }

  @SimpleProperty(
      description = "Lower notification priority, for items that are less important. The UI may " +
          "choose to show these items smaller, or at a different position in the list, compared " +
          "with your app's PriorityDefault items."
  )
  public int PriorityLow() {
    return NotificationCompat.PRIORITY_LOW;
  }

  @SimpleProperty(
      description = "Highest notification priority, for your application's most important items " +
          "that require the user's prompt attention or input."
  )
  public int PriorityMax() {
    return NotificationCompat.PRIORITY_MAX;
  }

  @SimpleProperty(
      description = "Lowest notification priority; these items might not be shown to the user " +
          "except under special circumstances, such as detailed notification logs."
  )
  public int PriorityMin() {
    return NotificationCompat.PRIORITY_MIN;
  }

  @SimpleProperty(
      description = "Default notification priority. If your application does not prioritize its " +
          "own notifications, use this value for all notifications."
  )
  public int PriorityDefault() {
    return NotificationCompat.PRIORITY_DEFAULT;
  }

  // Channel ID
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "MediaNotificationChannelID"
  )
  @SimpleProperty(
      description = "Specifies the channel the notification should be delivered on."
  )
  public void ChannelID(String id) {
    this.channelId = id;
  }

  @SimpleProperty(
      description = "Returns the channel the notifications are being delivered on."
  )
  public String ChannelID() {
    return this.channelId;
  }

  // Channel name
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "MediaNotificationChannel"
  )
  @SimpleProperty(
      description = "Specifies the name of channel the notification should be delivered on."
  )
  public void ChannelName(String name) {
    this.channelName = name;
  }

  @SimpleProperty(
      description = "Returns the name of channel the notifications are being delivered on."
  )
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
  @SimpleProperty(
      description = "Specifies the importance of the channel the notifications are being delivered on."
  )
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

  @SimpleProperty(
      description = "Returns the importance of the channel the notifications are being delivered on."
  )
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
