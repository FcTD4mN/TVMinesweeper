package com.damn.tvminesweeper.activities;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Damien on 28/01/2017.
 */

public class SettingsActivity extends Activity
{
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        getFragmentManager().beginTransaction()
                            .replace( android.R.id.content, new SettingsFragment() )
                            .commit();
    }
}
