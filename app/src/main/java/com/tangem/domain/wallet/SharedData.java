package com.tangem.domain.wallet;

import java.math.BigDecimal;
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

    public BigDecimal payload;
    public synchronized boolean UpdatePayload(BigDecimal value)
    {

        if(payload == null || payload.compareTo(value)!=0)
        {
            boolean isChange = true;
            if(payload == null)
                isChange = false;
            if(value!=BigDecimal.ZERO)
                payload = value;

            return isChange;
        }

        return false;
    }

    public void SetErrorRequest(int val)
    {
            errorRequest = new AtomicInteger(val);
    }

    public SharedData(int requstCount)
    {
        payload = BigDecimal.ZERO;
        allRequest = requstCount;
        errorRequest = new AtomicInteger(0);
        requestCounter = new AtomicInteger(0);
    }
}