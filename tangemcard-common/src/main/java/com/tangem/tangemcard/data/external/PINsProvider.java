package com.tangem.tangemcard.data.external;

import java.util.List;

public interface PINsProvider {
    String getPIN2();

    void setLastUsedPIN(String pin);

    List<String> getPINs();
}
