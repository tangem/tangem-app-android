package com.ripple.crypto.keys;

public interface IKeyPair extends IVerifyingKey {
    byte[] privateKey();
    byte[] signMessage(byte[] message);
}
