package com.tangem.domain.wallet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Ilia on 12.04.2018.
 */


public class SharedData
{
    public static int COUNT_REQUEST = 3;
    public AtomicInteger requestCounter;
    public int allRequest;
    public AtomicInteger errorRequest;

    public SharedData(int requstCount)
    {
        allRequest = requstCount;
        errorRequest = new AtomicInteger(0);
        requestCounter = new AtomicInteger(0);
    }
}