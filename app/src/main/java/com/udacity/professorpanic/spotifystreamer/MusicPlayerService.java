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

import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{
    private MediaPlayer mPlayer;
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private String artistName;
    private int chosenTrack;
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String PASSED_ARTIST_NAME = "Artist Name";


    public MusicPlayerService() {
    }

    public void onCreate()
    {
        super.onCreate();
        chosenTrack=0;
        mPlayer = new MediaPlayer();
        initMediaPlayer();
    }

    public void initMediaPlayer()
    {
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    public void setPlayerData(Bundle args)
    {
        Bundle bundle = new Bundle(args);

        this.topTracks = (ArrayList<Track>)bundle.get(TRACK_LIST);
        this.chosenTrack = bundle.getInt(CHOSEN_TRACK);
        this.artistName = bundle.getString(PASSED_ARTIST_NAME);
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
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        
    }
}
