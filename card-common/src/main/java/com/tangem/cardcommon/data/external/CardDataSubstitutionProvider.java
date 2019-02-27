package com.tangem.cardcommon.data.external;

import com.tangem.cardcommon.data.TangemCard;

/**
 * This interface provide method to make substitution of read card data (token symbol, contract address)
 * if they was unknown when the card was produced
 */
public interface CardDataSubstitutionProvider {
    void applySubstitution(TangemCard card);
}
