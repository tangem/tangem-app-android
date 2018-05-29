package com.tangem.domain.wallet;

/**
 * Created by Ilia on 29.09.2017.
 */

@SuppressWarnings("WeakerAccess")
public final class BitcoinException extends Exception {
    public static final int ERR_NO_SPENDABLE_OUTPUTS_FOR_THE_ADDRESS = 0;
    public static final int ERR_INSUFFICIENT_FUNDS = 1;
    public static final int ERR_WRONG_TYPE = 2;
    public static final int ERR_BAD_FORMAT = 3;
    public static final int ERR_INCORRECT_PASSWORD = 4;
    public static final int ERR_MEANINGLESS_OPERATION = 5;
    public static final int ERR_NO_INPUT = 6;
    public static final int ERR_FEE_IS_TOO_BIG = 7;
    public static final int ERR_FEE_IS_LESS_THEN_ZERO = 8;
    public static final int ERR_CHANGE_IS_LESS_THEN_ZERO = 9;
    public static final int ERR_AMOUNT_TO_SEND_IS_LESS_THEN_ZERO = 10;
    public static final int ERR_UNSUPPORTED = 11;

    public final int errorCode;
    @SuppressWarnings({"WeakerAccess", "unused"})
    public final Object extraInformation;

    public BitcoinException(int errorCode, String detailMessage, Object extraInformation) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.extraInformation = extraInformation;
    }

    public BitcoinException(int errorCode, String detailMessage) {
        this(errorCode, detailMessage, null);
    }
}
