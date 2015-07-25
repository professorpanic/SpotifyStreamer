package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by DoctorMondo on 7/14/2015.
 */
public class ArtistAdapter extends ArrayAdapter {

    Context mContext;
    int resourceId;
    List<Artist> artistList;

    public ArtistAdapter(Context context, int resource, List<Artist> objects) {
        super(context, resource, objects);
        mContext=context;
        resourceId=resource;
        artistList=objects;
    }




    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        TextView artistName;
        ImageView albumImage;


        if (v==null)
        {
            v = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.artist_list_item, null);
        }



        Artist artist = artistList.get(position);

        if (artist!=null)
        {
            artistName = (TextView) v.findViewById(R.id.artist_name_textview);
            albumImage = (ImageView) v.findViewById(R.id.album_preview_imageview);

            artistName.setText(artist.name);

            if (artist.images.size() > 0)
            {
                if (artist.images.size() > 2)
                {
                    Picasso.with(getContext()).load(artist.images.get(1).url).into(albumImage);
                }
            }

        }

        return v;
    }

}
