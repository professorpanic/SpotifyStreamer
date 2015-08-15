package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

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
    public static String START_FOREGROUND = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.startforeground";
    public static final String ACTION_PLAY_OR_PAUSE = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_play_or_pause";
    public static final String ACTION_STOP = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_stop";
    public static final String ACTION_NEXT = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_next";
    public static final String ACTION_PREV = "com.com.udacity.professorpanic.spotifystreamer.MusicPlayerService.action_prev";
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String TAG = "MusicPlayerService";
    private static final String ARTIST_ID = "Spotify artist ID";
    private static final String ARTIST_NAME = "Artist Name";
    private static final String TRACK_URI = "track URI";

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
        generateForegroundNotification();
        initMediaPlayer();
        return playerBinder;
    }

    private void generateForegroundNotification() {

        String nowPlayingArtist = args.getString(MediaPlayerFragment.PASSED_ARTIST_NAME);
        String nowPlayingSong = args.getString(MediaPlayerFragment.SONG_TITLE);
        if (mRemoteView == null)
        {
            mRemoteView = new RemoteViews(getPackageName(), R.layout.notification_layout_bar);
        }

        Intent notificationIntent = new Intent(getApplicationContext(), ArtistsMainActivity.class);
        notificationIntent.putExtra("nowPlayingArtist", nowPlayingArtist);
        notificationIntent.putExtra("nowPlayingSong", nowPlayingSong);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);



        Intent prevIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 100, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_prev, prevPendingIntent);

        Intent nextIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 200, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_next, nextPendingIntent);

        Intent pauseOrPlayIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
        pauseOrPlayIntent.setAction(ACTION_PLAY_OR_PAUSE);
        PendingIntent pauseOrPlayPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 300, pauseOrPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_pause_play, pauseOrPlayPendingIntent);

        Intent stopIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 400, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notification_stop, stopPendingIntent);
        //setNotificationListeners(mRemoteView);
        //mRemoteView.setTextViewText(R.id.notification_name, nowPlayingArtist);
        //mRemoteView.setTextViewText(R.id.notification_song, nowPlayingSong);
        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                // Set Icon
                .setSmallIcon(R.drawable.ic_music_note)
                        // Set Ticker Message
                .setTicker(getString(R.string.app_name))
                        // Dismiss Notification
                .setAutoCancel(true)
                        // Set PendingIntent into Notification
                .setContentIntent(pi)
                        // Set RemoteViews into Notification
                .setContent(mRemoteView);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        startForeground(NOTIFICATION_ID, notification);
//        // Create Notification Manager
//        NotificationManager notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        // Build Notification with Notification Manager
//        notificationmanager.notify(0, builder.build());
//
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        builder.setContent(mRemoteView);
//        builder.setContentIntent(pi);
//        builder.setStyle(new NotificationCompat.MediaStyle());
//        builder.setContentTitle(nowPlayingSong);
//        builder.setContentText(nowPlayingArtist);
//        builder.setSmallIcon(R.drawable.ic_action_play_arrow);
//        builder.addAction(R.drawable.ic_action_skip_previous, "Previous", prevPendingIntent);
//        builder.addAction(R.drawable.ic_action_pause, "Pause", pauseOrPlayPendingIntent);
//        builder.addAction(R.drawable.ic_action_stop, "Stop", stopPendingIntent);
//        builder.addAction(R.drawable.ic_action_skip_next, "Next", nextPendingIntent);
//
//        Notification notification = builder.build();
//        notification.flags = Notification.FLAG_ONGOING_EVENT;
//
//
//        notification.contentView = mRemoteView;
//
//
//        startForeground(NOTIFICATION_ID, notification);


    }

    private void setNotificationListeners(RemoteViews mRemoteView)
    {



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
            Log.i(TAG, "WOOOO RECEIVING A BROADCAST OR SOMETHING");

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
