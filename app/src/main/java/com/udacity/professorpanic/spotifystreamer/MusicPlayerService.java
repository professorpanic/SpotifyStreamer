package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener{

    public static final String SERVICE_RESULT = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.REQUEST_PROCESSED";

    public static final String SONG_POSITION = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.SONG_POSITION";
    public static final String SONG_DURATION = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.SONG_DURATION";
    public static final String SERVICE_IS_PLAYING = "com.udacity.professorpanic.spotifystreamer.MusicPlayerService.SERVICE_IS_PLAYING";
    private MediaPlayer mPlayer;
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private int chosenTrack;
    private int mBufferPosition;
    private Callbacks mCallbacks;
    private Bundle args = new Bundle();
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String TAG = "MusicPlayerService";
    private static final String ARTIST_ID = "Spotify artist ID";
    private static final String TRACK_URI = "track URI";
    private String artistId;
    private final PlayerBinder playerBinder = new PlayerBinder();
    private final static int SEEKBAR_UPDATE_INTERVAL = 1000;
    private LocalBroadcastManager broadcaster;
    private Thread seekbarUpdaterThread;

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
        initMediaPlayer();
        return playerBinder;
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
}
