package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by DoctorMondo on 7/14/2015.
 */
public class ArtistDetailAdapter extends ArrayAdapter {


        Context mContext;
        int resourceId;
        Album trackList;
        ArrayList<Track> adapterTopTracks;

        public ArtistDetailAdapter(Context context, int resource, ArrayList<Track> objects) {
            super(context, resource, objects);
            mContext=context;
            resourceId=resource;
            adapterTopTracks =objects;
        }




        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            TextView trackName;
            ImageView trackImage;
            TextView albumName;


            if (v==null)
            {
                v = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.detail_list_item, parent, false);
            }

            Track track = adapterTopTracks.get(position);

            if (track!=null)
            {
                trackName = (TextView) v.findViewById(R.id.track_name_textview);
                trackImage = (ImageView) v.findViewById(R.id.track_imageview);
                albumName = (TextView) v.findViewById(R.id.album_name_textview);


                trackName.setText("Track: "+ track.name);
                albumName.setText("Album: " + track.album.name);



                if (track.album.images !=null)
                {

                    if (track.album.images.size()> 0)
                    {
                        if (track.album.images.size() > 2)
                        {
                            Picasso.with(getContext()).load(track.album.images.get(1).url).into(trackImage);
                        }
                    }
                    else
                    {
                        trackImage.setImageResource(R.drawable.ic_broken_image_black_24dp);
                    }
                }



            }

            return v;
        }

    }

