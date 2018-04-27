package com.damn.tvminesweeper.activities;

import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.os.Bundle;

import com.damn.tvminesweeper.R;

import java.util.Map;

public class SettingsFragment extends PreferenceFragment
                                implements SharedPreferences.OnSharedPreferenceChangeListener
{
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Load the preferences from an XML resource
        addPreferencesFromResource( R.xml.minesweeper_settings );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        sharedPreferences = getPreferenceManager().getSharedPreferences();
        // we want to watch the preference values' changes
        sharedPreferences.registerOnSharedPreferenceChangeListener( this );

        // We set all summaries to be the current value
        Map< String, ? > allPrefs = sharedPreferences.getAll();
        for( Map.Entry< String, ? > entry : allPrefs.entrySet() )
        {
            try
            {
                EditTextPreference text = ( EditTextPreference ) findPreference( entry.getKey() );
                String value = sharedPreferences.getString( entry.getKey(), "-1" );
                if( text != null )
                    text.setSummary( value );
            }
            catch( ClassCastException e )
            {
            }
        }
    }

    @Override
    public void onPause()
    {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        try
        {
            EditTextPreference text = ( EditTextPreference ) findPreference( key );
            String value = sharedPreferences.getString( key, "-1" );
            if( text != null )
                text.setSummary( value );
        }
        catch( ClassCastException e )
        {
        }
    }
}

