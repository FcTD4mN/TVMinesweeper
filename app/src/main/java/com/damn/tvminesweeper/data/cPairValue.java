package com.damn.tvminesweeper.data;

/**
 * Created by Damien on 11/02/2017.
 */

public class cPairValue
{

    public cPairValue( int iKey, int iValue )
    {
        mKey = iKey;
        mValue = iValue;
    }

    public  int Value()
    {
        return  mValue;
    }

    public  int Key()
    {
        return  mKey;
    }

    private int     mKey;
    private int   mValue;
}
