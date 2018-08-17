package com.tangem.domain.wallet;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Ilia on 12.04.2018.
 */

public class SharedData {
    public static int COUNT_REQUEST = 3;
    public AtomicInteger requestCounter;
    public int allRequest;
    public AtomicInteger errorRequest;
    private BigDecimal payload;

    public SharedData(int requestCount) {
        payload = BigDecimal.ZERO;
        allRequest = requestCount;
        errorRequest = new AtomicInteger(0);
        requestCounter = new AtomicInteger(0);
    }

    public synchronized boolean updatePayload(BigDecimal value) {
        if (payload == null || payload.compareTo(value) != 0) {
            boolean isChange = true;
            if (payload == null || payload.equals(BigDecimal.ZERO))
                isChange = false;
            if (!value.equals(BigDecimal.ZERO))
                payload = value;
            else
                isChange = false;

            return isChange;
        }

        return false;
    }

    public void setErrorRequest(int val) {
        errorRequest = new AtomicInteger(val);
    }

}