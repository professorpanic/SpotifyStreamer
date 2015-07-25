package com.udacity.professorpanic.spotifystreamer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistsMainFragment extends ListFragment {

    private ArtistAdapter adapter;
    private String queryString;
    private ArrayList<Artist> artists = new ArrayList<Artist>();
    private static final String TAG = "ArtistActivityFragment";
    private Callbacks mCallbacks;

    public ArtistsMainFragment() {

    }

    public interface Callbacks
    {
        void onArtistSelected(ArtistDetailFragment fragment);

    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        mCallbacks = (Callbacks)activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallbacks=null;
    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        setRetainInstance(true);
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().setTitle(getString(R.string.app_name));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String artistId = ((Artist)adapter.getItem(position)).id;
        Bundle args = new Bundle();
        args.putString(getString(R.string.EXTRA_ARTIST), artistId);
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        fragment.setArguments(args);
        mCallbacks.onArtistSelected(fragment);
//        getActivity().getSupportFragmentManager().beginTransaction()
//                .replace(R.id.detail_fragment_container, fragment)
//                .addToBackStack(null)
//                .commit();
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artists_main, container, false);
        SpotifyApi api;

        ListView lv = (ListView)rootView.findViewById(android.R.id.list);
        adapter = new ArtistAdapter(getActivity(), R.layout.artist_list_item, artists);
        lv.setAdapter(adapter);

        final SearchView searchView = (SearchView) rootView.findViewById(R.id.search_artist);

        api = new SpotifyApi();
        final SpotifyService spotifyService = api.getService();

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                //this will check if there's a network connection before making calls to the spotify api. if it's false, we'll pop a toast and return false to the listener.
                //otherwise, proceed as normal.
                if (!Utility.isNetworkAvailable(getActivity().getApplicationContext()))
                {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.no_network_error), Toast.LENGTH_SHORT).show();
                    return false;
                }

                spotifyService.searchArtists(query, new Callback<ArtistsPager>() {
                    @Override
                    public void success(ArtistsPager artistsPager, Response response) {

                        List<Artist> artistNames = artistsPager.artists.items;

                            FetchArtistsTask fetchArtistsTask = new FetchArtistsTask();
                            fetchArtistsTask.execute(artistNames);

                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.v(TAG, error.toString());
                    }
                });

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        };

        searchView.setOnQueryTextListener(queryTextListener);

        return rootView;
    }

    public class FetchArtistsTask extends AsyncTask<List<Artist>, Void, Void>
    {
        List<Artist> artistList;

        @Override
        protected Void doInBackground(List<Artist>... params)
        {
            artistList = params[0];

            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {
            super.onPostExecute(v);

             if (artistList.size()>0)
            {
                adapter.clear();
                for (Artist item : artistList)
                {
                    adapter.add(item);
                }
            }
            else
            {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.no_artist_found), Toast.LENGTH_SHORT).show();
                adapter.clear();
            }
            adapter.notifyDataSetChanged();
        }
    }




}
