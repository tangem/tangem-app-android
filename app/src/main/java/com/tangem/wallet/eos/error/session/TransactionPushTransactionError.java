package com.tangem.wallet.eos.error.session;

import org.jetbrains.annotations.NotNull;

/**
 * Error class is used when there is an exception while attempting to call pushTransaction() of TransactionProcessor
 */
public class TransactionPushTransactionError extends TransactionProcessorError {

    public TransactionPushTransactionError() {
    }

    public TransactionPushTransactionError(@NotNull String message) {
        super(message);
    }

    public TransactionPushTransactionError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public TransactionPushTransactionError(@NotNull Exception exception) {
        super(exception);
    }
}
