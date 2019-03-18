package com.ripple.core.types.known.generic;

import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;

public class Validation extends STObject {
    public static boolean isValidation(STObject source) {
        return source.has(UInt32.LedgerSequence) &&
                source.has(UInt32.SigningTime) &&
                source.has(Hash256.LedgerHash) &&
                source.has(Blob.Signature);
    }
}
