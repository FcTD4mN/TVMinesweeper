package com.damn.tvminesweeper.data;

import java.util.Random;
import java.util.Vector;

/**
 * Created by Damien on 26/01/2017.
 */

public class Minesweeper
{
    // Set to -20 so that after neighbours increments and after a play, that increased by 10, we can say that negative values are mines,
    // and positives are mine count around.
    // This allows no more computation to be done apart from incrementing eachtime a mine is around a spot.
    private static final int MINESINITIALVALUE = -20;

    public enum ePlayMode
    {
        kPlacingFlags,
        kOpeningSpots
    };

    /** ===============================================================Construction / Destruction */
    public Minesweeper( int iTotalMines, int iWidth, int iHeight )
    {
        mGameArray = new int[ iWidth ][ iHeight ];
        for( int i = 0; i < mWidth ; ++i )
            for( int j = 0; j < mHeight ; ++j )
                mGameArray[ i ][ j ] = 0;

        mGameInitialized = false;
        mGameOver = false;
        mTotalMines = iTotalMines;
        mWidth = iWidth;
        mHeight = iHeight;
        mAutoRestart = false;
        mPlayMode = ePlayMode.kOpeningSpots;
        mClearSpotsLeft = mWidth * mHeight - mTotalMines;

        mInvalid = false;
    }

    /** ====================================================================Accessors / Modifiers */
    public boolean GameOver()
    {
        return  mGameOver;
    }

    public void TotalMines( int mTotalMines )
    {
        this.mTotalMines = mTotalMines;
    }

    public int TotalMines()
    {
        return  mTotalMines;
    }

    public int ClearSpotsLeft()
    {
        return  mClearSpotsLeft;
    }

    public int Width()
    {
        return mWidth;
    }

    public void Width( int mWidth )
    {
        this.mWidth = mWidth;
    }

    public int Height()
    {
        return mHeight;
    }

    public void Height( int mHeight )
    {
        this.mHeight = mHeight;
    }

    public void AutoRestart( boolean mAutoRestart )
    {
        this.mAutoRestart = mAutoRestart;
    }

    public void Invalid()
    {
        this.mInvalid = true;
    }

    public  ePlayMode PlayMode()
    {
        return  mPlayMode;
    }

    public  boolean AutoRestart()
    {
        return  mAutoRestart;
    }

    public  boolean FlagAction()
    {
        return  mFlagAction;
    }

    public void SwitchGameMode()
    {
        if( mPlayMode == ePlayMode.kPlacingFlags )
            mPlayMode = ePlayMode.kOpeningSpots;
        else
            mPlayMode = ePlayMode.kPlacingFlags;

    }

    /** =====================================================================================Game */
    public boolean Game()
    {
        return  false;
    }

    public Vector< cPairValue > Play( int iValue )
    {
        int X = iValue % mWidth;
        int Y = iValue / mWidth;
        Vector< cPairValue > allOpenedSpots = new Vector<>();

        if( mGameOver )
            return  allOpenedSpots;

        if( !mGameInitialized )
            InitializeGame( X, Y );

        // Number code is : negative = mines
        //                  0 - 10   is still hidden spots
        //                  10 - 20  is unveiled spots
        //                  >100     are flags
        // This allows for fast computations and less IFs

        mFlagAction = false;

        if( mPlayMode == ePlayMode.kPlacingFlags )
        {
            // If the spot has already been opened, and it was a number, you can no longer place a flag there, so we test play isn't between 10 and 20
            if( mGameArray[ X ][ Y ] < 10 || mGameArray[ X ][ Y ] > 20 )
            {
                PlaceFlag( allOpenedSpots, X, Y );
                mFlagAction = true;
            }
            else // If you click on it though, you can use the multiclear technique
            {
                OpenNearbySpots( allOpenedSpots, X, Y );
                mFlagAction = false;
            }
        }
        else
        {
            // If empty spot, we open recursively till we found numbers
            if( mGameArray[ X ][ Y ] == 0 )
                RecursiveOpening( allOpenedSpots, X, Y );
            else if( mGameArray[ X ][ Y ] < 10 ) // If not an already unveiled spot
                UnveilNumberSpot( allOpenedSpots, X, Y );
            else if( mGameArray[ X ][ Y ] > 10 && mGameArray[ X ][ Y ] < 20 )
                OpenNearbySpots( allOpenedSpots, X, Y );
        }

        return  allOpenedSpots;
    }

    public Vector< cPairValue > RevealGame()
    {
        Vector< cPairValue > allOpenedSpots = new Vector<>();

        for( int x = 0 ; x < mWidth ; ++x )
        {
            for( int y = 0 ; y < mHeight ; ++y )
            {
                // Remove any flag
                if( mGameArray[ x ][ y ] > 100 )
                    mGameArray[ x ][ y ] -= 150;

                // If empty spot, we open recursively till we found numbers
                if( mGameArray[ x ][ y ] == 0 )
                    RecursiveOpening( allOpenedSpots, x, y );
                else if( mGameArray[ x ][ y ] < 10 ) // If not an already unveiled spot
                    UnveilNumberSpot( allOpenedSpots, x, y );
            }
        }

        return  allOpenedSpots;
    }


    public  void  PlayForOpenNearbySpots( Vector< cPairValue > oAllOpenedSpots, int iX, int iY )
    {
        int spotContent = GetSpotContent( iX, iY );
        if( spotContent == 0 )
            RecursiveOpening( oAllOpenedSpots, iX, iY );
        else if( spotContent != -666 && spotContent < 10  ) // If not an already unveiled spot
            UnveilNumberSpot( oAllOpenedSpots, iX, iY );
    }

    public  void  OpenNearbySpots( Vector< cPairValue > oAllOpenedSpots, int iX, int iY )
    {
        if( GetSpotContent( iX, iY ) - 10 != FlagCount( iX, iY ) )
            return;

        PlayForOpenNearbySpots( oAllOpenedSpots, iX - 1, iY - 1 );
        PlayForOpenNearbySpots( oAllOpenedSpots, iX, iY - 1 );
        PlayForOpenNearbySpots( oAllOpenedSpots, iX + 1, iY - 1 );

        PlayForOpenNearbySpots( oAllOpenedSpots, iX - 1, iY );
        PlayForOpenNearbySpots( oAllOpenedSpots, iX + 1, iY );

        PlayForOpenNearbySpots( oAllOpenedSpots, iX - 1, iY + 1 );
        PlayForOpenNearbySpots( oAllOpenedSpots, iX, iY + 1 );
        PlayForOpenNearbySpots( oAllOpenedSpots, iX + 1, iY + 1 );
    }

    public  void  UnveilNumberSpot( Vector< cPairValue > oAllOpenedSpots, int iX, int iY )
    {
        mGameArray[ iX ][ iY ] += 10;
        oAllOpenedSpots.add( new cPairValue( iX + iY * mWidth, mGameArray[ iX ][ iY ] ) );

        if( mGameArray[ iX ][ iY ] < 0 )
            mGameOver = true;
        else
            mClearSpotsLeft -= 1;
    }

    public  void  RecursiveOpening( Vector< cPairValue > oAllOpenedSpots, int iX, int iY )
    {
        UnveilNumberSpot( oAllOpenedSpots, iX, iY );

        //Top
        if( iY > 0 )
        {
            if( mGameArray[ iX ][ iY - 1 ] > 0 && mGameArray[ iX ][ iY - 1 ] < 10 )
                UnveilNumberSpot( oAllOpenedSpots, iX, iY - 1 );
            else if( mGameArray[ iX ][ iY - 1 ] == 0 )
                RecursiveOpening( oAllOpenedSpots, iX, iY - 1 );

            if( iX > 0 )
            {
                if( mGameArray[ iX - 1 ][ iY - 1 ] > 0 && mGameArray[ iX - 1 ][ iY - 1 ] < 10 )
                    UnveilNumberSpot( oAllOpenedSpots, iX - 1, iY - 1 );
                else if( mGameArray[ iX - 1 ][ iY - 1 ] == 0 )
                    RecursiveOpening( oAllOpenedSpots, iX - 1, iY - 1 );
            }

            if( iX < mWidth - 1 )
            {
                if( mGameArray[ iX + 1 ][ iY - 1 ] > 0 && mGameArray[ iX + 1 ][ iY - 1 ] < 10 )
                    UnveilNumberSpot( oAllOpenedSpots, iX + 1, iY - 1 );
                else if( mGameArray[ iX + 1 ][ iY - 1 ] == 0 )
                    RecursiveOpening( oAllOpenedSpots, iX + 1, iY - 1 );
            }
        }

        //Bottom
        if( iY < mHeight - 1 )
        {
            if( mGameArray[ iX ][ iY + 1 ] > 0 && mGameArray[ iX ][ iY + 1 ] < 10 )
                UnveilNumberSpot( oAllOpenedSpots, iX, iY + 1 );
            else if( mGameArray[ iX ][ iY + 1 ] == 0 )
                RecursiveOpening( oAllOpenedSpots, iX, iY + 1 );

            if( iX > 0 )
            {
                if( mGameArray[ iX - 1 ][ iY + 1 ] > 0 && mGameArray[ iX - 1 ][ iY + 1 ] < 10 )
                    UnveilNumberSpot( oAllOpenedSpots, iX - 1, iY + 1 );
                else if( mGameArray[ iX - 1 ][ iY + 1 ] == 0 )
                    RecursiveOpening( oAllOpenedSpots, iX - 1, iY + 1 );
            }

            if( iX < mWidth - 1 )
            {
                if( mGameArray[ iX + 1 ][ iY + 1 ] > 0 && mGameArray[ iX + 1 ][ iY + 1 ] < 10 )
                    UnveilNumberSpot( oAllOpenedSpots, iX + 1, iY + 1 );
                else if( mGameArray[ iX + 1 ][ iY + 1 ] == 0 )
                    RecursiveOpening( oAllOpenedSpots, iX + 1, iY + 1 );
            }
        }

        //Left
        if( iX > 0 )
        {
            if( mGameArray[ iX - 1 ][ iY ] > 0 && mGameArray[ iX - 1 ][ iY ] < 10 )
                UnveilNumberSpot( oAllOpenedSpots, iX - 1, iY );
            else if( mGameArray[ iX - 1 ][ iY ] == 0 )
                RecursiveOpening( oAllOpenedSpots, iX - 1, iY );
        }

        //Right
        if( iX < mWidth - 1 )
        {
            if( mGameArray[ iX + 1 ][ iY ] > 0 && mGameArray[ iX + 1 ][ iY ] < 10 )
                UnveilNumberSpot( oAllOpenedSpots, iX + 1, iY );
            else if( mGameArray[ iX + 1 ][ iY ] == 0 )
                RecursiveOpening( oAllOpenedSpots, iX + 1, iY );
        }
    }

    public  void  PlaceFlag( Vector< cPairValue > oAllOpenedSpots, int iX, int iY )
    {
        if( mGameArray[ iX ][ iY ] >= 100 )
            mGameArray[ iX ][ iY ] -= 150;
        else
            mGameArray[ iX ][ iY ] += 150;

        oAllOpenedSpots.add( new cPairValue( iX + iY * mWidth, mGameArray[ iX ][ iY ] ) );
    }

    public  int  GetSpotContent( int iX, int iY )
    {
        if( iX >= 0 && iX < mWidth
                && iY >= 0 && iY < mHeight )
            return  mGameArray[ iX ][ iY ];

        return  -666;
    }

    public  int  FlagCount( int iX, int iY )
    {
        int flagCount = 0;

        if( GetSpotContent( iX - 1, iY - 1 ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX, iY - 1 ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX + 1, iY - 1 ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX - 1, iY ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX + 1, iY ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX - 1, iY + 1 ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX, iY + 1 ) > 100 )
            ++flagCount;
        if( GetSpotContent( iX + 1, iY + 1 ) > 100 )
            ++flagCount;

        return  flagCount;
    }

    public  void  Reset()
    {
        mGameArray = new int[ mWidth ][ mHeight ];
        for( int i = 0; i < mWidth ; ++i )
            for( int j = 0; j < mHeight ; ++j )
                mGameArray[ i ][ j ] = 0;

        mInvalid = false;
        mGameInitialized = false;
        mGameOver = false;
        mClearSpotsLeft = mWidth * mHeight - mTotalMines;
        // We go back to placing mines, because when you start fresh, placing flags is useless AF
        mPlayMode = ePlayMode.kOpeningSpots;
    }

    /** Initialize the game, taking the first play from the player into account so you can't lose right away */
    public  void InitializeGame( int iX, int iY )
    {
        Random rng = new Random( );
        java.util.Vector< Integer > alreadyPlacedMines = new java.util.Vector<>();

        // Setting up mines
        for( int i = 0 ; i < mTotalMines ; ++i )
        {
            int minePosition;
            do
                minePosition = rng.nextInt( mWidth * mHeight );
            while( minePosition == iX + iY * mWidth || alreadyPlacedMines.contains( minePosition ) );
            alreadyPlacedMines.add( minePosition );

            int xInGame = minePosition % mWidth;
            int yInGame = minePosition / mWidth;

            mGameArray[ xInGame ][ yInGame ] = MINESINITIALVALUE;
        }

        // Setting up numbers
        for( int i : alreadyPlacedMines )
        {
            int xInGame = i % mWidth;
            int yInGame = i / mWidth;

            IncreaseNeighbours( xInGame, yInGame );
        }
        mGameInitialized = true;
    }

    public void  IncreaseNeighbours( int iX, int iY )
    {
        int neighbourX;
        int neighbourY;

        //Top
        if( iY > 0 )
        {
            neighbourY = iY - 1;
            neighbourX = iX;

            mGameArray[ neighbourX ][ neighbourY ]++;

            //TopLeft
            if( iX > 0 )
            {
                neighbourX = iX - 1;
                mGameArray[ neighbourX ][ neighbourY ]++;
            }

            //TopRight
            if( iX < mWidth - 1 )
            {
                neighbourX = iX + 1;
                mGameArray[ neighbourX ][ neighbourY ]++;
            }
        }

        //Bottom
        if( iY < mHeight - 1 )
        {
            neighbourY = iY + 1;
            neighbourX = iX;
            mGameArray[ neighbourX ][ neighbourY ]++;

            //BottomLeft
            if( iX > 0 )
            {
                neighbourX = iX - 1;
                mGameArray[ neighbourX ][ neighbourY ]++;
            }

            //BottomRight
            if( iX < mWidth - 1 )
            {
                neighbourX = iX + 1;
                mGameArray[ neighbourX ][ neighbourY ]++;
            }
        }

        //Left
        if( iX > 0 )
        {
            neighbourX = iX - 1;
            neighbourY = iY;
            mGameArray[ neighbourX ][ neighbourY ]++;
        }

        //Right
        if( iX < mWidth - 1 )
        {
            neighbourX = iX + 1;
            neighbourY = iY;
            mGameArray[ neighbourX ][ neighbourY ]++;
        }
    }


    /** Members */
    private  int[][]            mGameArray;
    private  boolean            mGameInitialized;
    private  boolean            mGameOver;

    private  int                mTotalMines;
    private  int                mClearSpotsLeft;
    private  int                mWidth;
    private  int                mHeight;
    private  boolean            mAutoRestart;
    private  ePlayMode          mPlayMode;
    private  boolean            mFlagAction;

    /** Tells the game its core data changed, and that it would need a restart */
    private  boolean            mInvalid;
}
