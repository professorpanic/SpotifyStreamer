package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener{

    public static final String SERVICE_RESULT = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.REQUEST_PROCESSED";
    public static final int NOTIFICATION_ID = 101;
    private final static int SEEKBAR_UPDATE_INTERVAL = 500;
    public static final String SONG_POSITION = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.SONG_POSITION";
    public static final String SONG_DURATION = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.SONG_DURATION";
    public static final String SERVICE_IS_PLAYING = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.SERVICE_IS_PLAYING";
    public static final String ACTION_PLAY_OR_PAUSE = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_play_or_pause";
    public static final String ACTION_STOP = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_stop";
    public static final String ACTION_NEXT = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_next";
    public static final String ACTION_PREV = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_prev";
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String TAG = "MusicPlayerService";
    private static final String ARTIST_ID = "Spotify artist ID";
    NotificationCompat.Builder mBuilder;
    private MediaPlayer mPlayer;
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private int chosenTrack;
    private int mBufferPosition;
    private Callbacks mCallbacks;
    private Bundle args = new Bundle();
    private String artistId;
    private final PlayerBinder playerBinder = new PlayerBinder();
    private LocalBroadcastManager broadcaster;
    private Thread seekbarUpdaterThread;
    private RemoteViews mRemoteView;
    Notification mNotification;
    private ImageView mNotificationAlbumCover;
    private String nowPlayingArtist;
    private String nowPlayingSong;
    private TextView mNotificationArtist;
    private TextView mNotificationSong;

    //from here to notifyUpdate is code specfically for updating the seekbar and the Playing boolean,
    // since relying on an IsPlaying value from the service bombs if it isn't connected yet.
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent)
    {
        setBufferPosition(percent * mp.getDuration() / 100);
    }

    private void setBufferPosition(int progress)
    {
        mBufferPosition = progress;
    }

    private void startUpdater() {
        seekbarUpdaterThread = new Thread(new Runnable() {
            @Override public void run() {
                Thread myCurrent = Thread.currentThread();
                while (seekbarUpdaterThread == myCurrent) {

                    try {
                        Thread.sleep(SEEKBAR_UPDATE_INTERVAL);
                        notifyUpdate();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Error in startUpdaterthread: " + e.getMessage());
                        break;
                    }
                }

            }
        });
        seekbarUpdaterThread.start();
    }

    private void stopUpdater()
    {
        seekbarUpdaterThread.interrupt();
    }

    private void notifyUpdate() {
        {
            if (mPlayer != null)
            {
                if (mPlayer.isPlaying())
                {
                    sendResult(mPlayer.getCurrentPosition(), mPlayer.getDuration(), mPlayer.isPlaying());
                }
            }
        }
    }


    public interface Callbacks
    {
        void onTrackChangedByService(int newTrack);
        void stop();
    }

    public void sendResult(int position, int duration, boolean isPlaying) {
        Intent intent = new Intent(SERVICE_RESULT);
        if(position >= 0 && duration >= 0)
        {
            intent.putExtra(SONG_POSITION, position);
            intent.putExtra(SONG_DURATION, duration);
            intent.putExtra(SERVICE_IS_PLAYING, isPlaying);
        }
        broadcaster.sendBroadcast(intent);
    }

    public void registerCallbackClient(Activity activity)
    {
        mCallbacks = (Callbacks)activity;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "in OnStartCommand");
        //initializing this just to be safe and avoid nulls
        chosenTrack=0;

        return super.onStartCommand(intent, START_NOT_STICKY, startId);
    }

    public MusicPlayerService() {
    }

    public String getCurrentPlayingTrack()
    {
        return topTracks.get(chosenTrack).uri;
    }

    public String getCurrentPlayingURL() {return  topTracks.get(chosenTrack).preview_url;}

    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        super.onCreate();
        mPlayer = new MediaPlayer();
        broadcaster = LocalBroadcastManager.getInstance(this);
        this.registerReceiver(MusicPlayerServiceBroadcastReceiver, new IntentFilter(ACTION_PLAY_OR_PAUSE));
        this.registerReceiver(MusicPlayerServiceBroadcastReceiver, new IntentFilter(ACTION_STOP));
        this.registerReceiver(MusicPlayerServiceBroadcastReceiver, new IntentFilter(ACTION_NEXT));
        this.registerReceiver(MusicPlayerServiceBroadcastReceiver, new IntentFilter(ACTION_PREV));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(MusicPlayerServiceBroadcastReceiver);
        super.onDestroy();
    }



    public void initMediaPlayer()
    {
        //method to make it easy for all the boilerplate stuff for using the mediaplayer
        Log.i(TAG, "init Media Player");


        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);

        topTracks =  args.getParcelableArrayList(TRACK_LIST);
        chosenTrack = args.getInt(CHOSEN_TRACK);
        artistId = args.getString(ARTIST_ID);
        trackUri = Uri.parse(topTracks.get(chosenTrack).preview_url);

        try
        {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        mPlayer.prepareAsync();

    }

    public MediaPlayer getPlayer()
    {
        return mPlayer;
    }


    public void nextTrack()
    {
        if (chosenTrack == (topTracks.size()-1))
        {
            chosenTrack = 0;
        }
        else
        {
            chosenTrack++;
        }

        trackUri = Uri.parse(topTracks.get(chosenTrack).preview_url);
        mPlayer.reset();
        try {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.prepareAsync();
        mCallbacks.onTrackChangedByService(chosenTrack);
        generateForegroundNotification();

    }

    public void playOrPauseTrack() {
        if (mPlayer.isPlaying())
        {
            mPlayer.pause();
        }

        else
        {
            mPlayer.start();
        }

    }

    public void previousTrack()
    {
        if (chosenTrack == 0)
        {
            chosenTrack = (topTracks.size()-1);
        }
        else
        {
            chosenTrack--;
        }
        trackUri = Uri.parse(topTracks.get(chosenTrack).preview_url);
        mPlayer.reset();
        try
        {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        mPlayer.prepareAsync();
        mCallbacks.onTrackChangedByService(chosenTrack);
        generateForegroundNotification();

    }



    public class PlayerBinder extends Binder
    {
        MusicPlayerService getService()
        {
            return MusicPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        args = intent.getExtras();
        initMediaPlayer();
        generateForegroundNotification();
        return playerBinder;
    }

    private void generateForegroundNotification() {

        nowPlayingArtist = args.getString(MediaPlayerFragment.PASSED_ARTIST_NAME);
        nowPlayingSong = topTracks.get(chosenTrack).name;

        if (mRemoteView == null)
        {
            mRemoteView = new RemoteViews(getPackageName(), R.layout.notification_layout_bar);
        }

        Intent notificationIntent = new Intent(getApplicationContext(), ArtistsMainActivity.class);
        notificationIntent.putExtra("nowPlayingArtist", nowPlayingArtist);
        notificationIntent.putExtra("nowPlayingSong", nowPlayingSong);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        setNotificationListeners(mRemoteView);
        mRemoteView.setTextViewText(R.id.notification_name, nowPlayingArtist);
        mRemoteView.setTextViewText(R.id.notification_song, nowPlayingSong);


        int lockScreen;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean lockPlayerOn = prefs.getBoolean("player_lock", false);
        if (lockPlayerOn)
        {
            lockScreen = Notification.VISIBILITY_PUBLIC;
        }
        else
        {
            lockScreen = Notification.VISIBILITY_SECRET;
        }


         mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                 //this notification will flip according to whether or not the user has the Display on Lock setting checked.
                 //this will not work if they don't actually have a secured lock screen.
                 .setVisibility(lockScreen)
                // Set Icon
                .setSmallIcon(R.drawable.ic_music_note)
                        // Set Ticker Message
                .setTicker(getString(R.string.app_name))
                        // Dismiss Notification
                .setAutoCancel(true)
                        // Set PendingIntent into Notification
                .setContentIntent(pi)
                 //set style so buttons aren't cut off
                 //.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.app_name)))
                 // Set RemoteViews into Notification
                .setContent(mRemoteView);



        Notification mNotification = mBuilder.build();
        try
        {
            Track track = topTracks.get(chosenTrack);
            Picasso.with(getApplicationContext()).load(track.album.images.get(1).url).into(mRemoteView,R.id.notification_album_img,NOTIFICATION_ID, mNotification);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        startForeground(NOTIFICATION_ID, mNotification);


    }

    private void setNotificationListeners(RemoteViews mRemoteView)
    {
        //pass a remoteview and it'll attach the PendingIntents for each of the notification's buttons.
        Intent prevIntent = new Intent(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 100, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_prev, prevPendingIntent);

        Intent nextIntent = new Intent(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 200, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_next, nextPendingIntent);

        Intent pauseOrPlayIntent = new Intent(ACTION_PLAY_OR_PAUSE);
        PendingIntent pauseOrPlayPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 300, pauseOrPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_pause_play, pauseOrPlayPendingIntent);

        Intent stopIntent = new Intent(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 400, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_stop, stopPendingIntent);


    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "in onCompletion");

        nextTrack();
    }

    @Override
    public boolean onUnbind(Intent intent){
        stopUpdater();
        mPlayer.stop();
        mPlayer.release();
        stopSelf();

        return false;
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "onPrepared");
        if (mPlayer.isPlaying()) {
            stopUpdater();
            mPlayer.stop();
            mPlayer.reset();
        }
        mPlayer.start();
        startUpdater();
    }

    public BroadcastReceiver MusicPlayerServiceBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            String action = intent.getAction();
            Log.i(TAG, "Broadcast Received");

            if(action.equals(ACTION_PLAY_OR_PAUSE))
            {
                playOrPauseTrack();
            }
            else if (action.equals(ACTION_NEXT))
            {
                nextTrack();
            }
            else if (action.equals(ACTION_PREV))
            {
                previousTrack();
            }
            else if (action.equals(ACTION_STOP))
            {
                mCallbacks.stop();
            }

        }

    };
}
