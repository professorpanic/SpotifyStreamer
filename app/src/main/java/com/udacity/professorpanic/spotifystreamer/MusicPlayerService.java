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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{
    private static final String TRACK_BUNDLE = "Contains TRACK_LIST, CHOSEN_TRACK, PASSED_ARTIST_NAME";
    static final public String PAUSE_OR_PLAY_REQUEST = "com.udacity.professorpanic.spotifystreamer.musicplayerservice.PAUSE_OR_PLAY_REQUEST_PROCESSED";
    static final public String PAUSE_OR_PLAY_MESSAGE = "com.udacity.professorpanic.spotifystreamer.musicplayerservice.PAUSE_OR_PLAY_MSG";
    static final public String UPDATE_UI_REQUEST = "com.udacity.professorpanic.spotifystreamer.musicplayerservice.UI_UPDATE_REQUEST_PROCESSED";
    private MediaPlayer mPlayer;
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private String artistName;
    private int chosenTrack;
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




    class CustomMediaPlayer extends MediaPlayer
    {
        @Override
        public boolean isPlaying() {
//            sendMessage(PAUSE_OR_PLAY_REQUEST);
            return super.isPlaying();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "in OnStartCommand");

        chosenTrack=0;

        //args = intent.getExtras();
        //initMediaPlayer();
        return super.onStartCommand(intent, START_NOT_STICKY, startId);
    }

    public MusicPlayerService() {
    }

    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        super.onCreate();
        mPlayer = new CustomMediaPlayer();
//        broadcaster = LocalBroadcastManager.getInstance(this);


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

//    private void sendMessage(String message)
//    {

//        Intent intent = new Intent(message);
//        // You can also include some extra data.
//        if (message.equals(PAUSE_OR_PLAY_REQUEST))
//        {
//            intent.putExtra(PAUSE_OR_PLAY_MESSAGE, mPlayer.isPlaying());
//        }
//        else if (message.equals((UPDATE_UI_REQUEST)))
//        {
//            intent.putExtra(CURRENT_PLAYING_TRACK, chosenTrack);
//        }
//        broadcaster.sendBroadcast(intent);
//    }

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
//        sendMessage(UPDATE_UI_REQUEST);
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
//        sendMessage(UPDATE_UI_REQUEST);
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
