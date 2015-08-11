package com.udacity.professorpanic.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.udacity.professorpanic.spotifystreamer.MusicPlayerService.PlayerBinder;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;


public class ArtistsMainActivity extends ActionBarActivity implements  ArtistsMainFragment.Callbacks, MusicPlayerService.Callbacks, ArtistDetailFragment.Callbacks, MediaPlayerFragment.Callbacks, MediaPlayerFragment.OnTopTracksSelectedListener{
    private boolean mTwoPane;
    private MusicPlayerService musicPlayerService;
    private boolean musicBound=false;
    private Bundle topTracksBundle;
    private static final String TAG="Main Activity";

    @Override
    public void onTopTracksSelected(Bundle args)
    {
        //this starts the new service, with args that come from the MediaPlayerFragment.
        topTracksBundle = args;

        Intent playIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
        playIntent.putExtras(topTracksBundle);
        int chosenTrack = topTracksBundle.getInt(MediaPlayerFragment.CHOSEN_TRACK);
        ArrayList<Track> topTracks = topTracksBundle.getParcelableArrayList(MediaPlayerFragment.TRACK_LIST);
        Log.i(TAG, "We are now starting the service");
               if (musicPlayerService != null)
               {
                   Log.i(TAG, musicPlayerService.getCurrentPlayingTrack() + " Is the current playing track");
                   Log.i(TAG, topTracks.get(chosenTrack).uri + " is the incoming track");
                   if (!musicPlayerService.getCurrentPlayingTrack().equals(topTracks.get(chosenTrack).uri))
                    //this is for starting a new track the user picks, if the service is in the middle of running. If the current track and this bundle do not much, user
                   //wants a new song. otherwise, don't kill and restart the service.
                   {

                       Log.i(TAG, "This kills the service.");
                       getApplicationContext().unbindService(musicConnection);
                   }

                }
                getApplicationContext().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
    }

    public MusicPlayerService getMusicPlayerService() {
        return musicPlayerService;
    }

    //connect to musicplayerservice
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "We are now connecting to the service");
            PlayerBinder binder = (PlayerBinder)service;
            //get service
            musicPlayerService = binder.getService();
            //pass list
            musicBound = true;
            //setting up so the service can send callbacks to the activity, for manipulating a fragment
            musicPlayerService.registerCallbackClient(ArtistsMainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        // checking to be sure that the fragment container has a fragment in it.
        if (findViewById(R.id.fragment_container) != null) {
            // if it does, we should check if's being restored from a previous
            // instance state.
            if (savedInstanceState != null) {
                return;
            }

            ArtistsMainFragment artistsMainFragment = new ArtistsMainFragment();

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, artistsMainFragment).commit();


        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artists_main, menu);
        return true;
    }

    protected int getLayoutResId()
    {
        return R.layout.activity_masterdetail;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //logic for uis depending on if it's a tablet or a phone
    @Override
    public void onArtistSelected(ArtistDetailFragment fragment) {
        if (findViewById(R.id.detail_fragment_container) == null)
        {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }

        else
        {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();

        }
    }
    @Override
    public void onArtistSelected(MediaPlayerFragment fragment) {
        fragment.show(getFragmentManager(), MediaPlayerFragment.TAG);
    }






    @Override
    public void nextTrack() {

        musicPlayerService.nextTrack();
    }

    @Override
    public void playOrPauseTrack() {
        musicPlayerService.playOrPauseTrack();

    }

    @Override
    public void previousTrack() {
        musicPlayerService.previousTrack();

    }

    @Override
    public void onTrackChangedByService(int newTrack) {
        MediaPlayerFragment fragment =  (MediaPlayerFragment)getFragmentManager().findFragmentByTag(MediaPlayerFragment.TAG);
        //this callback will update the dialog fragment if it exists and is visible, so the player info changes when the service moves to a new track.
        if (fragment != null)
        {
            if (fragment.isVisible())
            {
                fragment.updateUi(newTrack);
            }
        }
    }
}
