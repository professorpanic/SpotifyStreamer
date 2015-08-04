package com.udacity.professorpanic.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{
    private static final String TRACK_BUNDLE = "Contains TRACK_LIST, CHOSEN_TRACK, PASSED_ARTIST_NAME";
    private MediaPlayer mPlayer;
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private String artistName;
    private int chosenTrack;
    Bundle args;
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String PASSED_ARTIST_NAME = "Artist Name";
    private static final String TAG = "MusicPlayerService";
    private static final String ARTIST_ID = "Spotify artist ID";
    private static final String TRACK_URI = "track URI";
    private String artistId;
    private final PlayerBinder playerBinder = new PlayerBinder();




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "in OnStartCommand");
        args = intent.getExtras();
        chosenTrack=0;
        mPlayer = new MediaPlayer();

        initMediaPlayer();
        return super.onStartCommand(intent, flags, startId);
    }

    public MusicPlayerService() {
    }

    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        super.onCreate();



    }

    public void initMediaPlayer()
    {
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
        chosenTrack++;
        trackUri = Uri.parse(topTracks.get(chosenTrack).preview_url);
        mPlayer.reset();
        try {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.prepareAsync();

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
