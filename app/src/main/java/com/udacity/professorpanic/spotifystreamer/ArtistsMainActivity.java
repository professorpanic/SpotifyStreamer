package com.udacity.professorpanic.spotifystreamer;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class ArtistsMainActivity extends ActionBarActivity implements  ArtistsMainFragment.Callbacks, ArtistDetailFragment.Callbacks{
    private boolean mTwoPane;


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

            //hopefully doing it this way will make it easier for when it's time for to implement tablet compatibility, to make a master/detail flow.
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
        fragment.show(getFragmentManager(), "Test");
    }
}
