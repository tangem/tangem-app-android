package com.tangem.wallet.eos.error.session;

import com.tangem.wallet.eos.error.EosioError;

import org.jetbrains.annotations.NotNull;

/**
 * Error class is used when there is an exception while attempting to call any method of TransactionProcessor
 */
public class TransactionProcessorError extends EosioError {

    public TransactionProcessorError() {
    }

    public TransactionProcessorError(@NotNull String message) {
        super(message);
    }

    public TransactionProcessorError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public TransactionProcessorError(@NotNull Exception exception) {
        super(exception);
    }
}
