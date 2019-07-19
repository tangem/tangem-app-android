/*
 * Copyright (c) 2017-2019 block.one all rights reserved.
 */

package com.tangem.wallet.eos.error.utilities;

import com.tangem.wallet.eos.error.EosioError;

import org.jetbrains.annotations.NotNull;

/**
 * Error that originates from the {@link one.block.eosiojava.utilities.PEMProcessor} class.
 */
public class PEMProcessorError extends EosioError {

    public PEMProcessorError() {
    }

    public PEMProcessorError(@NotNull String message) {
        super(message);
    }

    public PEMProcessorError(@NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public PEMProcessorError(@NotNull Exception exception) {
        super(exception);
    }
}
