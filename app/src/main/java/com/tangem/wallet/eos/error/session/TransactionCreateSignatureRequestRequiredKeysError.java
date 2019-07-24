package com.tangem.wallet.eos.error.session;

import org.jetbrains.annotations.NotNull;

/**
 * Error class is used when there is an exception while attempting to call getRequiredKeys() inside
 * createSignatureRequest() of TransactionProcessor
 */
public class TransactionCreateSignatureRequestRequiredKeysError extends TransactionCreateSignatureRequestError {

    public TransactionCreateSignatureRequestRequiredKeysError() {
    }

    public TransactionCreateSignatureRequestRequiredKeysError(@NotNull String message) {
        super(message);
    }

    public TransactionCreateSignatureRequestRequiredKeysError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public TransactionCreateSignatureRequestRequiredKeysError(@NotNull Exception exception) {
        super(exception);
    }
}
