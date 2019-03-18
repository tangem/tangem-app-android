package com.ripple.crypto.ed25519;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;

public class ED25519 {
    static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable
                                .getByName("Ed25519");
}
