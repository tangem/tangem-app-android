package com.tangem.tangem_card.data.external;

import java.util.List;

/**
 * Interface of PINsProvider - object that know some list of PINs (used when start first time read), PIN2 (used for protected operation) and store last used PIN
 * to use it in following operations
 */
public interface PINsProvider {

    /**
     * @return list of known PINs
     * This PINs used when start reading of card
     * When start reading a PINs from this list used sequential in search PIN algorithm until right PIN found
     */
    List<String> getPINs();

    /**
     * @return PIN2 for protected operations
     */
    String getPIN2();


    /**
     * Call after successful first time reading of card to store founded PIN (normally this PIN must be returned in next time {@see getPINs} at first position)
     * @param pin
     */
    void setLastUsedPIN(String pin);

    byte[] getTerminalPublicKey();
    byte[] getTerminalPrivateKey();

}
