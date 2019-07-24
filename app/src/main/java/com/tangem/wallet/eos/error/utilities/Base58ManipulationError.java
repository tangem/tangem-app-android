package com.tangem.wallet.eos.error.utilities;

import com.tangem.wallet.eos.error.EosioError;

import org.jetbrains.annotations.NotNull;

/**
 * Error is thrown for exceptions that occur during Base58
 * encoding or decoding operations.
 */
public class Base58ManipulationError extends EosioError {
    public Base58ManipulationError() {
    }

    public Base58ManipulationError(@NotNull String message) {
        super(message);
    }

    public Base58ManipulationError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public Base58ManipulationError(@NotNull Exception exception) {
        super(exception);
    }
}
