package com.damn.tvminesweeper.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.damn.tvminesweeper.R;
import com.damn.tvminesweeper.data.Minesweeper;

import com.damn.tvminesweeper.data.cPairValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

public class GameActivity extends AppCompatActivity
                            implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public enum eState
    {
        kGameRunning,
        kGameOver
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled( true );
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_game );

        mState = eState.kGameRunning;

        // Loading settings icon as setting it as default on XML bugs in API 16
        Button settings = ( Button )findViewById( R.id.uiSettingsButton );
        settings.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_settings, null ) );


        // Putting all colors in memory
        mColors = new int[ 9 ];
        mColors[ 0 ] = Color.parseColor( "black" );
        mColors[ 1 ] = Color.parseColor( "blue" );
        mColors[ 2 ] = Color.parseColor( "green" );
        mColors[ 3 ] = Color.parseColor( "red" );
        mColors[ 4 ] = Color.parseColor( "#003399" );
        mColors[ 5 ] = Color.parseColor( "cyan" );
        mColors[ 6 ] = Color.parseColor( "#660033" );
        mColors[ 7 ] = Color.parseColor( "#001a33" );
        mColors[ 8 ] = Color.parseColor( "#505050" );

        // Settings
        PreferenceManager.setDefaultValues( this, R.xml.minesweeper_settings, false );
        SharedPreferences userSetting = PreferenceManager.getDefaultSharedPreferences( this );
        userSetting.registerOnSharedPreferenceChangeListener( this );

        int nbMine = Integer.parseInt( userSetting.getString( "resNbMines", "45" ) );
        int gameWidth = Integer.parseInt( userSetting.getString( "resGameWidth", "13" ) );
        int gameHeight = Integer.parseInt( userSetting.getString( "resGameHeight", "20" ) );
        boolean gameAutoRestart = userSetting.getBoolean( "resAutoRestart", false );

        // Main game
        mMainGame = new Minesweeper( nbMine, gameWidth, gameHeight );
        mMainGame.AutoRestart( gameAutoRestart );
        mBestTime = ReadBestTime();

        TextView timer = ( TextView )findViewById( R.id.uiTimer );
        mTimer = new cTimer( 1000, 1000, timer, mBestTime );

        mTimerStarted = false;
        mLongClick = false;

        // UI Setup
        ResetUIDisplay();
        final LinearLayout gameVerticalLayout = ( LinearLayout )findViewById( R.id.uiGameVerticalLayout );
        ViewTreeObserver vto = gameVerticalLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                gameVerticalLayout.getViewTreeObserver().removeOnGlobalLayoutListener( this );
                CreateButtons();
            }
        });
    }


    /** UI functions */
    public  void  CreateButtons()
    {
        LinearLayout gameVerticalLayout = ( LinearLayout )findViewById( R.id.uiGameVerticalLayout );
        LinearLayout topBar = ( LinearLayout )findViewById( R.id.uiTopBar );

        gameVerticalLayout.removeAllViews();
        gameVerticalLayout.setWeightSum( mMainGame.Height() );

        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics( display );
        int maxHeight = ( display.heightPixels - topBar.getHeight() ) / mMainGame.Height();
        int maxWidth = gameVerticalLayout.getWidth() / mMainGame.Width();
        int sideLength;
        if( maxHeight > maxWidth )
            sideLength = maxWidth;
        else
            sideLength = maxHeight;

        for( int i = 0 ; i < mMainGame.Height() ; ++i )
        {
            LinearLayout row = new LinearLayout( this );
            row.setOrientation( LinearLayout.HORIZONTAL );

            LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT );
            llParam.gravity = Gravity.CENTER_HORIZONTAL;
            llParam.weight = 1;
            row.setLayoutParams( llParam );

            row.setWeightSum( mMainGame.Width() );
            row.setBackground( ContextCompat.getDrawable( this, R.drawable.minesweeper_background_color ) );

            for( int j = 0 ; j < mMainGame.Width() ; ++j )
            {
                Button button = new Button( this );
                LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT );
                layoutParam.width = sideLength;
                layoutParam.height = sideLength;
                button.setLayoutParams( layoutParam );
                button.setId( j + i * mMainGame.Width() );
                button.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick( View v ) {
                        onGridButtonPressed( v );
                    }
                } );

                button.setOnLongClickListener( new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick( View v )
                    {
                        // Using a variable to transmit the information of a long click to the onGridButton function
                        mLongClick = true;
                        Vibrator vibrator = (Vibrator) v.getContext().getSystemService( Context.VIBRATOR_SERVICE );
                        vibrator.vibrate( 100 );
                        onGridButtonPressed( v );
                        // Return true apparently means that the event longClick handled the click, and won't send a onClick event afterwards
                        return true;
                    }
                });

                button.setBackground( ContextCompat.getDrawable( this, R.drawable.minesweeper_button ) );
                button.setPadding( 0, 0, 0, 0 );

                row.addView( button );
            }

            gameVerticalLayout.addView( row );
        }
    }

    public  void  ResetUIDisplay()
    {
        // Setting timer display
        TextView timer = ( TextView )findViewById( R.id.uiTimer );
        timer.setText( "00:00" );
        timer.setTextColor( Color.parseColor( "#FF0000" ) );

        // Setting bestTime display
        TextView bestTime = ( TextView )findViewById( R.id.uiBestTime );
        int seconds = mBestTime % 60;
        int minutes = mBestTime / 60;
        // There is yet no best time
        if( mBestTime == -1 )
        {
            seconds = 0;
            minutes = 0;
            bestTime.setTextColor( Color.parseColor( "#553333" ) );
        }
        else
        {
            bestTime.setTextColor( Color.parseColor( "#FF0000" ) );
        }

        NumberFormat f00 = new DecimalFormat("00");
        bestTime.setText( f00.format( minutes ) + ":" + f00.format( seconds ) );

        // Setting total mines number display
        TextView mineNumber = ( TextView )findViewById( R.id.uiMinesLeft );
        NumberFormat f000 = new DecimalFormat( "000" );
        mineNumber.setText( f000.format( mMainGame.TotalMines() ) );

        // Setting right icon for the mode button
        Button buttonMode = ( Button )findViewById( R.id.uiClickMode );
        if( mMainGame.PlayMode() == Minesweeper.ePlayMode.kPlacingFlags )
            buttonMode.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_flagstrokefill, null ) );
        else
            buttonMode.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_mine, null ) );

        // Resetting proper smiley
        Button buttonSmiley = ( Button )findViewById( R.id.uiRestart );
        buttonSmiley.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_smiley_play, null ) );
    }

    public  void  ResetGame()
    {
        mMainGame.Reset();
        CreateButtons();
        mTimerStarted = false;
        mTimer.cancel();
        mState = eState.kGameRunning;
        ResetUIDisplay();
    }

    /** Buttons events */
    /** Settings */
    public  void    onSettingsButtonClicked( View iView )
    {
        Intent settingsActivity = new Intent( GameActivity.this, SettingsActivity.class );
        startActivity( settingsActivity );
    }

    /** SmileyButton */
    public  void    onResetButtonPressed( View view )
    {
        ResetGame();
    }

    /** GridButton */
    public  void    onGridButtonPressed( View view )
    {
        if( mState == eState.kGameOver )
            return;

        Button button = ( Button )findViewById( view.getId() );

        if( !mTimerStarted )
        {
            mTimerStarted = true;
            TextView timerView = ( TextView )findViewById( R.id.uiTimer );

            mTimer = new cTimer( 6000000, 1000, timerView, mBestTime );
            mTimer.start();
        }
        if( mLongClick )
            mMainGame.SwitchGameMode();

        Vector< cPairValue > allPlays = mMainGame.Play( button.getId() );

        for( int i = 0 ; i < allPlays.size() ; ++i )
        {
            Button buttonToUpdate = ( Button )findViewById( allPlays.elementAt( i ).Key() );

            int play = allPlays.elementAt( i ).Value();
            if( mMainGame.PlayMode() == Minesweeper.ePlayMode.kPlacingFlags )
            {
                if( play >= 100 )
                {
                    buttonToUpdate.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_flagstrokefill, null ) );
                    TextView mineDisplay = ( TextView )findViewById( R.id.uiMinesLeft );
                    NumberFormat f = new DecimalFormat( "000" );
                    mineDisplay.setText( f.format( Integer.parseInt( mineDisplay.getText().toString() ) - 1 ) );
                }
                else if( mMainGame.FlagAction() )
                {
                    buttonToUpdate.setBackground( ContextCompat.getDrawable( this, R.drawable.minesweeper_button ) );
                    TextView mineDisplay = ( TextView )findViewById( R.id.uiMinesLeft );
                    NumberFormat f = new DecimalFormat( "000" );
                    mineDisplay.setText( f.format( Integer.parseInt( mineDisplay.getText().toString() ) + 1 ) );

                }
                else
                {
                    ProcessOpeningSpotGUI( buttonToUpdate, play, false );
                }
            }
            else
            {
                ProcessOpeningSpotGUI( buttonToUpdate, play, false );
            }
        }

        if( mLongClick )
        {
            mMainGame.SwitchGameMode();
            mLongClick = false;
        }
        if( mMainGame.ClearSpotsLeft() <= 0 && ! mMainGame.GameOver() )
            ProcessWinGame();

        if( mMainGame.GameOver() )
            ProcessGameOver();
    }

    /** BestTime */
    public  void    onBestTimeClicked( View view )
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public  void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    ClearBestTimes();
                    ResetUIDisplay();
                }
                else if( which == DialogInterface.BUTTON_NEGATIVE )
                {
                    // Do nothing
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder( view.getContext() );
        builder.setMessage( "Clear best times ?" ).setPositiveButton( "Yes", dialogClickListener ).setNegativeButton( "No", dialogClickListener ).show();
    }

    /** Processing */
    /** This methods updates the buttons UI to reflect the value */
    public  void    ProcessOpeningSpotGUI( Button iButton, int iPlay, boolean  iRevealingGame )
    {
        if( iPlay == 10 ) // Empty spot, wanna make it look like pushed
        {
            iButton.setBackground( ContextCompat.getDrawable( this, R.drawable.minesweeper_button_cleared ) );
        }
        else if( iPlay > 10 && iPlay < 20 ) // Actual number, wanna print it
        {
            iButton.setText( Integer.toString( iPlay - 10 ) );
            iButton.setBackground( ContextCompat.getDrawable( this, R.drawable.minesweeper_button_cleared ) );
            iButton.setTextColor( mColors[ iPlay - 10 ] );
        }
        else if( iPlay < 0 )
        {
            if( iRevealingGame )
                iButton.setBackground( ContextCompat.getDrawable( this, R.drawable.ic_mine ) );
            else
                iButton.setBackground( ContextCompat.getDrawable( this, R.drawable.ic_mine_red ) );
        }
    }

    public  void    ProcessGameOver()
    {
        if( mMainGame.AutoRestart() )
        {
            ResetGame();
        }
        else
        {
            mState = eState.kGameOver;
            mTimer.cancel();
            Toast toast;
            toast = Toast.makeText( getApplicationContext(), "You lost", Toast.LENGTH_LONG );

            // Setting smiley to lose
            Button buttonSmiley = ( Button )findViewById( R.id.uiRestart );
            buttonSmiley.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_smiley_lose, null ) );

            Vector< cPairValue > remainingSpots = mMainGame.RevealGame();
            for( int i = 0; i < remainingSpots.size() ; ++i )
            {
                Button buttonToUpdate = ( Button )findViewById( remainingSpots.elementAt( i ).Key() );
                int value = remainingSpots.elementAt( i ).Value();
                ProcessOpeningSpotGUI( buttonToUpdate, value, true );
            }

            toast.show();
        }
    }

    public  void    ProcessWinGame()
    {
        Toast toast;
        int currentTime = mTimer.Time();
        mTimer.cancel();

        // Setting smiley to lose
        Button buttonSmiley = ( Button )findViewById( R.id.uiRestart );
        buttonSmiley.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_smiley_win, null ) );

        if( mBestTime == -1 || currentTime < mBestTime )
        {
            toast = Toast.makeText( getApplicationContext(), "You won with a new record !", Toast.LENGTH_LONG );
            WriteBestTime( currentTime );
            mBestTime = currentTime;
        }
        else
        {
            int minutes = mBestTime / 60;
            int seconds = mBestTime % 60;
            NumberFormat f = new DecimalFormat( "00" );
            toast = Toast.makeText( getApplicationContext(), "You won. Previous time was " + f.format( minutes ) + ":" + f.format( seconds ) , Toast.LENGTH_LONG );
        }
        toast.show();
    }

    public  int     ReadBestTime()
    {
        Context context = getApplicationContext();
        int bestTime = -1;
        try
        {
            InputStream inputStream = context.openFileInput( "bestTime.txt" );

            if ( inputStream != null )
            {
                InputStreamReader inputStreamReader = new InputStreamReader( inputStream );
                BufferedReader bufferedReader = new BufferedReader( inputStreamReader );
                String receiveString;

                while ( ( receiveString = bufferedReader.readLine() ) != null )
                {
                    if( receiveString.matches( Integer.toString( mMainGame.Width() ) + "-" + Integer.toString( mMainGame.Height() ) + "-" + Integer.toString( mMainGame.TotalMines() ) + ":[0-9]*" ) )
                        break;
                }
                if( receiveString != null )
                {
                    String time = receiveString.substring( receiveString.indexOf( ":" ) + 1 );
                    inputStream.close();

                    bestTime = Integer.parseInt( time.toString() );
                }
            }
        }
        catch( FileNotFoundException e )
        {
            Log.e("login activity", "File not found: " + e.toString());
        }
        catch( IOException e )
        {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return  bestTime;
    }

    public  void    WriteBestTime( int iBestTime )
    {
        Context context = getApplicationContext();
        String fullFileContent = "";
        // We read file content first, so we cant then write new time (write = erase content) and appedn old content
        // so when we read, best time is 80% of times first value.
        // Point is to keep Read as fast as possible as it's used quite often
        try
        {
            InputStream inputStream = context.openFileInput( "bestTime.txt" );

            if ( inputStream != null )
            {
                InputStreamReader inputStreamReader = new InputStreamReader( inputStream );
                BufferedReader bufferedReader = new BufferedReader( inputStreamReader );
                String receiveString;

                while ( ( receiveString = bufferedReader.readLine() ) != null )
                    fullFileContent += receiveString + "\n";
            }
        }
        catch( FileNotFoundException e )
        {
            Log.e("login activity", "File not found: " + e.toString());
        }
        catch( IOException e )
        {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        try
        {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter( context.openFileOutput( "bestTime.txt", Context.MODE_PRIVATE ) );
            BufferedWriter writer = new BufferedWriter( outputStreamWriter );
            writer.write( Integer.toString( mMainGame.Width() ) + "-" + Integer.toString( mMainGame.Height() ) + "-" + Integer.toString( mMainGame.TotalMines() ) + ":" + Integer.toString( iBestTime ) );
            writer.newLine();
            for( String s : fullFileContent.split( "\n" ) )
            {
                writer.append( s );
                writer.newLine();
            }
            writer.close();
            outputStreamWriter.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString() );
            Toast toast = Toast.makeText( getApplicationContext(), "File write failed: " + e.toString(), Toast.LENGTH_SHORT );
            toast.show();
        }
    }

    public  void    ClearBestTimes()
    {
        Context context = getApplicationContext();
        try
        {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter( context.openFileOutput( "bestTime.txt", Context.MODE_PRIVATE ) );
            outputStreamWriter.write( "" );
            outputStreamWriter.close();
            mBestTime = -1;
            ResetUIDisplay();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString() );
            Toast toast = Toast.makeText( getApplicationContext(), "File write failed: " + e.toString(), Toast.LENGTH_SHORT );
            toast.show();
        }
    }

    public  void    onChangeModeButtonClicked( View view )
    {
        mMainGame.SwitchGameMode();
        Button buttonMode = ( Button )findViewById( R.id.uiClickMode );
        if( mMainGame.PlayMode() == Minesweeper.ePlayMode.kPlacingFlags )
            buttonMode.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_flagstrokefill, null ) );
        else
            buttonMode.setBackground( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_mine, null ) );
    }

    /** Preferences listener */
    @Override
    public  void    onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        SharedPreferences.Editor settingsEditor = sharedPreferences.edit();

        if( key.equals( "resNbMines" ) )
        {
            int nbMine = Integer.parseInt( sharedPreferences.getString( "resNbMines", "45" ) );
            if( nbMine < 1 )
            {
                nbMine = 1;
                settingsEditor.putString( "resNbMines", "1" );
                settingsEditor.commit();
            }
            else if( nbMine > mMainGame.Width() * mMainGame.Height() - 1 )
            {
                nbMine = mMainGame.Width() * mMainGame.Height() - 1;
                settingsEditor.putString( "resNbMines", Integer.toString( nbMine ) );
                settingsEditor.commit();
            }

            mMainGame.TotalMines( nbMine );
            mMainGame.Invalid();
            mBestTime = ReadBestTime();
            ResetGame();
        }
        else if( key.equals( "resGameWidth" ) )
        {
            int gameWidth = Integer.parseInt( sharedPreferences.getString( "resGameWidth", "13" ) );
            if( gameWidth < 2 )
            {
                gameWidth = 2;
                settingsEditor.putString( "resGameWidth", "2" );
                settingsEditor.commit();
            }

            mMainGame.Width( gameWidth );
            mMainGame.Invalid();
            mBestTime = ReadBestTime();

            int nbMine = Integer.parseInt( sharedPreferences.getString( "resNbMines", "45" ) );
            if( nbMine > mMainGame.Width() * mMainGame.Height() - 1 )
            {
                nbMine = mMainGame.Width() * mMainGame.Height() - 1;
                settingsEditor.putString( "resNbMines", Integer.toString( nbMine ) );
                settingsEditor.commit();
            }

            ResetGame();
        }
        else if( key.equals( "resGameHeight" ) )
        {
            int gameHeight = Integer.parseInt( sharedPreferences.getString( "resGameHeight", "20" ) );
            if( gameHeight < 2 )
            {
                gameHeight = 2;
                settingsEditor.putString( "resGameHeight", "2" );
                settingsEditor.commit();
            }

            mMainGame.Height( gameHeight );
            mMainGame.Invalid();
            mBestTime = ReadBestTime();

            int nbMine = Integer.parseInt( sharedPreferences.getString( "resNbMines", "45" ) );
            if( nbMine > mMainGame.Width() * mMainGame.Height() - 1 )
            {
                nbMine = mMainGame.Width() * mMainGame.Height() - 1;
                settingsEditor.putString( "resNbMines", Integer.toString( nbMine ) );
                settingsEditor.commit();
            }
            ResetGame();
        }
        else if( key.equals( "resAutoRestart" ) )
        {
            mMainGame.AutoRestart( sharedPreferences.getBoolean( key, false ) );
        }
    }


    /** TIMER */
    public class cTimer extends CountDownTimer
    {
        public cTimer( long millisInFuture, long countDownInterval, TextView iTimerView, int iBestTime )
        {
            super( millisInFuture, countDownInterval );
            mTimerView = iTimerView;
            // Setting time to -1 so that when game starts, it doesn't show 00:01 instantly
            mTime = -1;
            mBestTime = iBestTime;
        }

        @Override
        public void onTick( long millisUntilFinished )
        {
            mTime += 1;
            int seconds = mTime % 60;
            int minutes = mTime / 60;

            NumberFormat f = new DecimalFormat("00");
            mTimerView.setText( f.format( minutes ) + ":" + f.format( seconds ) );

            int color = Color.parseColor( "#00FF00" );
            int deltaTime = mBestTime - mTime;

            if( mBestTime >= 0 )
            {
                if( deltaTime > 10 )
                    color = Color.parseColor( "#00FF00" );
                else if( deltaTime <= 10 && deltaTime >= 0 )
                    color = Color.parseColor( "#FFFF00" );
                else
                    color = Color.parseColor( "#FF0000" );
            }

            mTimerView.setTextColor( color );
        }

        @Override
        public void onFinish()
        {
            mMainGame.Reset();
            CreateButtons();
            ResetUIDisplay();
            mTimerStarted = false;
        }

        public  int  Time()
        {
            return  mTime;
        }


        private  TextView   mTimerView;
        private  int        mTime;
        private  int        mBestTime;
    }

    private  Minesweeper    mMainGame;
    private  int            mBestTime;
    private  boolean        mTimerStarted;
    private  cTimer         mTimer;
    private  int[]          mColors;
    private  eState         mState;
    private  boolean        mLongClick;

}
