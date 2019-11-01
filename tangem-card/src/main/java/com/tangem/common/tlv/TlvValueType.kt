package com.tangem.common.tlv

enum class TlvValueType {
    HexString,
    Utf8String,
    IntValue,
    BoolValue,
    ByteArray,
    EllipticCurve,
    DateTime,
    ProductMask,
    SettingsMask,
    CardStatus,
    SigningMethod
}