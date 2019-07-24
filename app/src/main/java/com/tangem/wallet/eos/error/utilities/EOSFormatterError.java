

package com.tangem.wallet.eos.error.utilities;

import com.tangem.wallet.eos.error.EosioError;

import org.jetbrains.annotations.NotNull;

/**
 * Error class is used when there is an exception while attempting to call any method of EOSFormatter
 */
public class EOSFormatterError extends EosioError {

    public EOSFormatterError() {
    }

    public EOSFormatterError(@NotNull String message) {
        super(message);
    }

    public EOSFormatterError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public EOSFormatterError(@NotNull Exception exception) {
        super(exception);
    }
}
