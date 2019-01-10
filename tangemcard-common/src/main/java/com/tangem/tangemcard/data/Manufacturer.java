package com.tangem.tangemcard.data;

/**
 * Created by dvol on 09.08.2017.
 */

public enum Manufacturer {

    Unknown("", "Unknown"),
    SMARTCASH_AG("SMART CASH AG", "SMART CASH AG"),
    DEVELOPERS_SMARTCASH_AG("DEVELOP CASH AG", "SMART CASH AG (DEVELOPERS)"),
    SMARTCASH("SMART CASH", "SMART CASH");

    private String ID;
    private String officialName;

    Manufacturer(String id, String officialName) {
        this.ID = id;
        this.officialName = officialName;
    }

    public String getOfficialName() {
        return officialName;
    }

    public static Manufacturer FindManufacturer(String ID) {
        Manufacturer[] manufacturers = Manufacturer.values();
        for (int i = 1; i < manufacturers.length; i++) {
            if (manufacturers[i].ID.equals(ID)) {
                return manufacturers[i];
            }
        }
        return Manufacturer.Unknown;
    }
}
