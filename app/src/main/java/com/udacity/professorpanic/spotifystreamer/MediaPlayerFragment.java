package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
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
    private final static String TAG = "MediaPlayerFragment";
    private ArrayList<Track> topTracks;
    private Uri trackUri;
    private OnTopTracksSelectedListener mOnTopTracksListener;
    private OnNextTrackListener mNextTrackCallback;
    private Callbacks mCallbacks;
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

    private static final String CHOSEN_TRACK = "Chosen Track";
    private static final String PASSED_ARTIST_NAME = "Artist Name";
    private static final String TRACK_LIST = "Artist Top Ten Tracks";
    private static final String ARTIST_ID = "Spotify Artist ID";
    private static final String TRACK_URI = "Track Uri";
    private MediaController mController;
    private Handler mHandler = new Handler();
    SeekBar seekBar;
    Bundle args;

    public ArrayList<Track> getTopTracks() {return this.topTracks;}

    public interface OnTopTracksSelectedListener
    {
        void onTopTracksSelected(Bundle args);
    }

    public interface OnNextTrackListener
    {
        void nextTrack();
    }

    public interface Callbacks
    {
        void nextTrack();

        void playOrPauseTrack();

        void previousTrack();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mOnTopTracksListener = (OnTopTracksSelectedListener) activity;
            mCallbacks = (Callbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTopTracksSelectedListener");
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



    }

    @Override
    public void onStart()
    {
        super.onStart();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(MusicPlayerService.PAUSE_OR_PLAY_REQUEST);
//        intentFilter.addAction(MusicPlayerService.UPDATE_UI_REQUEST);
//                LocalBroadcastManager.getInstance(getActivity()).
//                        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onStop() {
//        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onStop();
    }

//    private final BroadcastReceiver receiver = new BroadcastReceiver()
//    {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction().equalsIgnoreCase(MusicPlayerService.PAUSE_OR_PLAY_REQUEST))
//            {
//                Log.i(TAG, "now in onReceive");
//                servicePlaying = intent.getBooleanExtra(MusicPlayerService.PAUSE_OR_PLAY_MESSAGE, false);
//                updateUi(chosenTrack);
//            }
//
//            if (intent.getAction().equalsIgnoreCase((MusicPlayerService.UPDATE_UI_REQUEST)))
//            {
//                chosenTrack = intent.getIntExtra(MusicPlayerService.CURRENT_PLAYING_TRACK, 0);
//                updateUi(chosenTrack);
//            }
//        }
//    };
    public void updateUi(int trackNumber)
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

            mOnTopTracksListener.onTopTracksSelected(intentArgs);




        trackImageView = (ImageView) rootView.findViewById(R.id.mediaplayer_image);

        artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText(artistName);

        albumNameTextView = (TextView) rootView.findViewById(R.id.player_artist_album);
        albumNameTextView.setText(topTracks.get(chosenTrack).album.name);

        trackNameTextView = (TextView) rootView.findViewById(R.id.player_artist_track);
        trackNameTextView.setText(topTracks.get(chosenTrack).name);

        seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);


        playButton = (ImageButton) rootView.findViewById(R.id.play_button);
        updateUi(chosenTrack);




        playButton.setImageResource(R.drawable.ic_action_pause);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (servicePlaying) {
                    playButton.setImageResource(R.drawable.ic_action_play_arrow);
                    mCallbacks.playOrPauseTrack();
                } else {
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
