package com.tangem.wallet;

import java.io.Serializable;

/**
 * Created by Ilia on 07.01.2018.
 */

public interface RLPElement extends Serializable {

    byte[] getRLPData();
}
