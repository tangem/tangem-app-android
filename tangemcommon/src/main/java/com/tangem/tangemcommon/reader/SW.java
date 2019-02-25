package com.tangem.tangemcommon.reader;

/**
 * Created by dvol on 07.03.2018.
 */

public class SW {
    public static final int PROCESS_COMPLETED = 0x9000;
    public static final int INVALID_PARAMS = 0x6A86;
    public static final int ERROR_PROCESSING_COMMAND = 0x6286;
    public static final int INVALID_STATE = 0x6985;
    public static final int PINS_NOT_CHANGED = PROCESS_COMPLETED;
    public static final int PIN1_CHANGED = PROCESS_COMPLETED + 0x0001;
    public static final int PIN2_CHANGED = PROCESS_COMPLETED + 0x0002;
    public static final int PINS_CHANGED = PROCESS_COMPLETED + 0x0003;
    public static final int INS_NOT_SUPPORTED = 0x6D00;
    public static final int NEED_ENCRYPTION = 0x6982;
    public static final int NEED_PAUSE = 0x9789;

    public static String getDescription(int sw) {
        switch (sw) {
            case ERROR_PROCESSING_COMMAND:
                return "SW_ERROR_PROCESSING_COMMAND";
            case INVALID_PARAMS:
                return "SW_INVALID_PARAMS";
            case INVALID_STATE:
                return "SW_INVALID_STATE";
            case INS_NOT_SUPPORTED:
                return "SW_INS_NOT_SUPPORTED";
            case NEED_ENCRYPTION:
                return "SW_NEED_ENCRYPTION";
            case PIN1_CHANGED:
                return "SW_PIN1_CHANGED";
            case PIN2_CHANGED:
                return "SW_PIN2_CHANGED";
            case PINS_CHANGED:
                return "SW_PINS_CHANGED";
            case PROCESS_COMPLETED:
                return "SW_PROCESS_COMPLETED";
        }
        return "???";
    }
}
