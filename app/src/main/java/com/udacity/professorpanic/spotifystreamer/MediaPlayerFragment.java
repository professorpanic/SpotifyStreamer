package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by DoctorMondo on 7/15/2015.
 */
public class MediaPlayerFragment extends DialogFragment {
    public final static String TAG = "MediaPlayerFragment";
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private OnTopTracksSelectedListener mOnTopTracksListener;
    private Callbacks mCallbacks;
    private int position;
    private int duration;
    ImageView trackImageView;
    TextView artistNameTextView;
    TextView trackNameTextView;
    View rootView;
    TextView albumNameTextView;
    ImageButton playButton;
    ImageButton skipNextButton;
    ImageButton skipPreviousButton;
    private String artistName;
    private String artistId;
    private int chosenTrack=0;
    private boolean servicePlaying = false;
    private BroadcastReceiver receiver;
    public static final String CHOSEN_TRACK = "Chosen Track";
    public static final String PASSED_ARTIST_NAME = "Artist Name";
    public static final String TRACK_LIST = "Artist Top Ten Tracks";
    public static final String ARTIST_ID = "Spotify Artist ID";

    SeekBar seekBar;
    Bundle args;





    public interface OnTopTracksSelectedListener
    {
        void onTopTracksSelected(Bundle args);
    }

    public void updateSeekBar(int position, int duration)
    {
       if (seekBar != null)
       {
        seekBar.setMax(duration);
        seekBar.setProgress(position);
       }
    }



    public interface Callbacks
    {
        void nextTrack();

        void playOrPauseTrack();

        void previousTrack();

        void seekBarChanged(SeekBar seekBar,int progress,boolean fromUser);

        boolean isPlaying();


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try
        {
            mOnTopTracksListener = (OnTopTracksSelectedListener) activity;
            mCallbacks = (Callbacks) activity;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(activity.toString()+ " must implement OnTopTracksSelectedListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        args = this.getArguments();
        topTracks = (ArrayList<Track>)args.get(TRACK_LIST);
        artistName = args.getString(PASSED_ARTIST_NAME);
        artistId = args.getString(ARTIST_ID);
        chosenTrack = args.getInt(CHOSEN_TRACK);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                position = intent.getIntExtra(MusicPlayerService.SONG_POSITION, 0);
                duration = intent.getIntExtra(MusicPlayerService.SONG_DURATION, 0);
                updateSeekBar(position, duration);
            }
        };
    }

    @Override
    public void onStart()
    {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver((receiver), new IntentFilter(MusicPlayerService.SERVICE_RESULT));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(receiver);
        super.onStop();
    }


    public void updateUi(int trackNumber)
    {
        //method for updating the dialogfragment's displayed into. There's no way for the service to get the track arraylist without it coming from this
        //class first, so we can assume this is using the same Track arraylist the service has.
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
            seekBar.setMax((int)(topTracks.get(trackNumber).duration_ms) / 1000);



        }

        Log.i(TAG, artistName);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.media_player_layout, container, false);



            Log.i(TAG, "in the onCreateView try");
            Bundle intentArgs = new Bundle();
            intentArgs.putString(ARTIST_ID, artistId);
            intentArgs.putInt(CHOSEN_TRACK, chosenTrack);
            intentArgs.putParcelableArrayList(TRACK_LIST, topTracks);

            //if this view's being created, that means the uuser wants to play music. This callback will throw the track arraylist back to the
            //main activity, which will in turn start the service.
            mOnTopTracksListener.onTopTracksSelected(intentArgs);




        trackImageView = (ImageView) rootView.findViewById(R.id.mediaplayer_image);

        artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText(artistName);

        albumNameTextView = (TextView) rootView.findViewById(R.id.player_artist_album);
        albumNameTextView.setText(topTracks.get(chosenTrack).album.name);

        trackNameTextView = (TextView) rootView.findViewById(R.id.player_artist_track);
        trackNameTextView.setText(topTracks.get(chosenTrack).name);

        seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mCallbacks.seekBarChanged(seekBar, progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        playButton = (ImageButton) rootView.findViewById(R.id.play_button);
        updateUi(chosenTrack);
        if (mCallbacks.isPlaying())
        {
            playButton.setImageResource(R.drawable.ic_action_pause);
        }
        else
        {
            playButton.setImageResource(R.drawable.ic_action_play_arrow);
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks.isPlaying())
                {
                    playButton.setImageResource(R.drawable.ic_action_play_arrow);
                    mCallbacks.playOrPauseTrack();
                }
                else
                {
                    playButton.setImageResource(R.drawable.ic_action_pause);
                    mCallbacks.playOrPauseTrack();
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
                    updateUi(chosenTrack);
                    mCallbacks.nextTrack();
                }
                else
                {
                    chosenTrack++;
                    updateUi(chosenTrack);
                    mCallbacks.nextTrack();
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
                    updateUi(chosenTrack);
                    mCallbacks.previousTrack();
                }
                else
                {
                    chosenTrack--;
                    updateUi(chosenTrack);
                    mCallbacks.previousTrack();
                }

            }
        });


        for (Track t : topTracks)
        {
            Log.i(TAG, t.name.toString());
        }


        return rootView;
    }

}
