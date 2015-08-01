package com.udacity.professorpanic.spotifystreamer;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kaaes.spotify.webapi.android.models.Artist;
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
    public ComponentName startService(Intent service) {
        Log.i(TAG, "in Start Service");

        return super.startService(service);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "in OnStartCommand");
        args = intent.getExtras();
        chosenTrack=0;
        mPlayer = new MediaPlayer();
        Log.i(TAG, "onCreate");
        initMediaPlayer();
        return super.onStartCommand(intent, flags, startId);
    }

    public MusicPlayerService() {
    }

    public void onCreate()
    {
        super.onCreate();



    }

    public void initMediaPlayer()
    {
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);

        chosenTrack = (int)args.get("Chosen Track");
        artistId = (String)args.get("Spotify Artist ID");
        String uriString = (String)args.get("Track Uri");
        Log.e(TAG, uriString);
        trackUri = Uri.parse(uriString);


        try {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.prepareAsync();

        onPrepared(mPlayer);
        Log.i(TAG, "init Media Player");
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
