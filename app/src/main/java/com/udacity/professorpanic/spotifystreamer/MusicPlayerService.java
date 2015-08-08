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

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{

    private MediaPlayer mPlayer;
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private String artistName;
    private int chosenTrack;
    private Callbacks mCallbacks;
    private Bundle args = new Bundle();
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String CHOSEN_TRACK = "Chosen Track";
    public static final String CURRENT_PLAYING_TRACK = "Now Playing";
    private static final String PASSED_ARTIST_NAME = "Artist Name";
    private static final String TAG = "MusicPlayerService";
    private static final String ARTIST_ID = "Spotify artist ID";
    private static final String TRACK_URI = "track URI";
    private String artistId;
    private final PlayerBinder playerBinder = new PlayerBinder();
    private LocalBroadcastManager broadcaster;






    public interface Callbacks
    {
        void onTrackChangedByService(int newTrack);
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

    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        super.onCreate();
        mPlayer = new MediaPlayer();
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


        try {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.prepareAsync();



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
        try {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
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
        if (mPlayer.isPlaying())
        {
            mPlayer.stop();
            mPlayer.reset();
        }


        mPlayer.start();

    }
}
