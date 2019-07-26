package com.tangem.wallet.eos.error.session;

import org.jetbrains.annotations.NotNull;

/**
 * Error class is used when there is an exception while attempting make any RPC calls inside
 * prepare() of TransactionProcessor
 */
public class TransactionPrepareRpcError extends TransactionPrepareError {

    public TransactionPrepareRpcError() {
    }

    public TransactionPrepareRpcError(@NotNull String message) {
        super(message);
    }

    public TransactionPrepareRpcError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public TransactionPrepareRpcError(@NotNull Exception exception) {
        super(exception);
    }
}
