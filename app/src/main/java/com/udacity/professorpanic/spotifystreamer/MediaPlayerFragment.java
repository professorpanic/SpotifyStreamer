package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.app.DialogFragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by DoctorMondo on 7/15/2015.
 */
public class MediaPlayerFragment extends DialogFragment implements MediaController.MediaPlayerControl, MediaPlayer.OnPreparedListener, View.OnTouchListener{
    private final static String TAG = "MediaPlayerFragment";
    private MediaPlayer mPlayer = new MediaPlayer();
    private ArrayList<Track> topTracks;
    private Uri trackUri;

    ImageView trackImageView;
    TextView artistNameTextView;
    TextView trackNameTextView;
    View rootView;
    TextView albumNameTextView;
    ImageButton playButton;
    ImageButton skipNextButton;
    ImageButton skipPreviousButton;
    private String artistName;
    private int chosenTrack=0;
    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String PASSED_ARTIST_NAME = "Artist Name";
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private MediaController mController;
    private Handler mHandler = new Handler();
    SeekBar seekBar;

    public MediaPlayer getMediaPlayer() {
        return this.mPlayer;
    }

    public ArrayList<Track> getTopTracks() {return this.topTracks;}



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = this.getArguments();
        topTracks = (ArrayList<Track>)args.get(TRACK_LIST);
        artistName = args.getString(PASSED_ARTIST_NAME);

        chosenTrack = args.getInt(CHOSEN_TRACK);
        mController = new MediaController(getActivity()) {
            @Override
            public void hide() {}      // This bit is to keep the controller from hiding after 3 seconds

            //this is to close out both the mediacontroller and player in one swoop
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    Activity a = (Activity)getContext();
                    a.finish();

                }
                return true;
            }
        };

       // Bundle intentBundle = intent.getExtras();
        mPlayer.reset();
        mPlayer.setOnPreparedListener(this);





    }

    public void updatePlayer(int trackNumber)
    {



        if (!Utility.isNetworkAvailable(getActivity().getApplicationContext()))
        {

            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.no_network_error), Toast.LENGTH_SHORT).show();
            trackImageView.setImageResource(R.drawable.ic_music_note);
        }
        else
        {
            albumNameTextView.setText(topTracks.get(trackNumber).album.name);
            artistNameTextView.setText(artistName);
            trackNameTextView.setText(topTracks.get(trackNumber).name);
            Picasso.with(getActivity()).load(topTracks.get(trackNumber).album.images.get(0).url).into(trackImageView);

        }

        Log.i(TAG, artistName);
        try {

            if (mPlayer.isPlaying())
            {
                mPlayer.stop();
                mPlayer.reset();
            }

            trackUri = Uri.parse(topTracks.get(chosenTrack).preview_url);
            Log.e(TAG, trackUri.toString());

            mPlayer.setDataSource(getActivity(), trackUri);
            mPlayer.prepare();
            mPlayer.start();
            seekBar.setMax(mPlayer.getDuration());
        } catch (IOException e) {
            Log.e(TAG, "Could not open file for playback. trackUri is " + trackUri.toString(), e);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.media_player_layout, container, false);


//        try {
//
//            trackUri = Uri.parse(topTracks.get(chosenTrack).preview_url);
//            mPlayer.setDataSource(getActivity(), trackUri);
//            mPlayer.prepare();
//            mPlayer.start();
//        } catch (IOException e) {
//            Log.e(TAG, "Could not open file for playback. trackUri is " + trackUri.toString(), e);
//        }

        trackImageView = (ImageView) rootView.findViewById(R.id.mediaplayer_image);

        artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText(artistName);

        albumNameTextView = (TextView) rootView.findViewById(R.id.player_artist_album);
        albumNameTextView.setText(topTracks.get(chosenTrack).album.name);

        trackNameTextView = (TextView) rootView.findViewById(R.id.player_artist_track);
        trackNameTextView.setText(topTracks.get(chosenTrack).name);

        seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);


        playButton = (ImageButton) rootView.findViewById(R.id.play_button);
        updatePlayer(chosenTrack);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {

                    mPlayer.seekTo(progress);

                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        playButton.setImageResource(R.drawable.ic_action_pause);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer.isPlaying()) {
                    playButton.setImageResource(R.drawable.ic_action_play_arrow);
                    mPlayer.pause();
                } else {
                    playButton.setImageResource(R.drawable.ic_action_pause);
                    mPlayer.start();
                }

            }
        });
        skipNextButton = (ImageButton) rootView.findViewById(R.id.skip_track_button);
        skipNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosenTrack >= topTracks.size()-1)
                {
                    chosenTrack=0;
                    updatePlayer(chosenTrack);
                }
                else
                {
                    chosenTrack++;
                    updatePlayer(chosenTrack);
                }
            }
        });

        skipPreviousButton = (ImageButton) rootView.findViewById(R.id.previous_track_button);
        skipPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosenTrack == 0)
                {
                    chosenTrack=(topTracks.size())-1;
                    updatePlayer(chosenTrack);
                }
                else
                {
                    chosenTrack--;
                    updatePlayer(chosenTrack);
                }

            }
        });


        for (Track t : topTracks)
        {
            Log.i(TAG, t.name.toString());
        }


        return rootView;
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mController.hide();
        mPlayer.stop();
        mPlayer.release();

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mController.show();
        Log.i(TAG, "onTouch");
        return false;
    }



//These are for the MediaPlayer


    @Override
    public void start() {
        mPlayer.start();
        playButton.setImageResource(R.drawable.ic_action_pause);
        Log.i(TAG, "onStart");
        Handler handler = new Handler();
        MediaPlayerFragment.this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null)
                {
                    int currentPos = mPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(currentPos);
                }
                mHandler.postDelayed(this, 1000);
            }
        });
    }



    @Override
    public void pause() {

        mPlayer.pause();

    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();

    }

    @Override
    public void seekTo(int pos) {
        mPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {

        return mPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {


        mController.setMediaPlayer(this);

        mController.setAnchorView(rootView);
        mController.requestFocus();

        mHandler.post(new Runnable() {
            public void run() {
                mController.setEnabled(true);
                mController.show(0);
            }
        });
        mController.show(0);
    }





}
