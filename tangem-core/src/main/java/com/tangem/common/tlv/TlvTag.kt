package com.tangem.common.tlv

/**
 * Contains all possible value types that value for [TlvTag] can contain.
 */
enum class TlvValueType {
    HexString,
    HexStringToHash,
    Utf8String,
    Uint16,
    Uint32,
    BoolValue,
    ByteArray,
    EllipticCurve,
    DateTime,
    ProductMask,
    SettingsMask,
    CardStatus,
    SigningMethod,
    IssuerDataMode
}

/**
 * Contains all TLV tags, with their code and descriptive name.
 */
enum class TlvTag(val code: Int) {
    Unknown(0x00),
    CardId(0x01),
    Status(0x02),
    CardPublicKey(0x03),
    CardSignature(0x04),
    CurveId(0x05),
    HashAlgID(0x06),
    SigningMethod(0x07),
    MaxSignatures(0x08),
    PauseBeforePin2(0x09),
    SettingsMask(0x0A),
    CardData(0x0C),
    NdefData(0x0D),
    CreateWalletAtPersonalize(0x0E),
    Health(0x0F),

    Pin(0x10),
    Pin2(0x11),
    NewPin(0x12),
    NewPin2(0x13),
    NewPinHash(0x14),
    NewPin2Hash(0x15),
    Challenge(0x16),
    Salt(0x17),
    ValidationCounter(0x18),
    Cvc(0x19),

    SessionKeyA(0x1A),
    SessionKeyB(0x1B),
    Pause(0x1C),
    NewPin3(0x1E),
    CrExKey(0x1F),

    Uid(0x0B),

    ManufactureId(0x20),
    ManufacturerSignature(0x86),

    IssuerDataPublicKey(0x30),
    IssuerTransactionPublicKey(0x31),
    IssuerData(0x32),
    IssuerDataSignature(0x33),
    IssuerTransactionSignature(0x34),
    IssuerDataCounter(0x35),
    AcquirerPublicKey(0x37),

    Size(0x25),
    Mode(0x23),
    Offset(0x24),

    IsActivated(0x3A),
    ActivationSeed(0x3B),
    ResetPin(0x36),

    CodePageAddress(0x40),
    CodePageCount(0x41),
    CodeHash(0x42),

    TransactionOutHash(0x50),
    TransactionOutHashSize(0x51),
    TransactionOutRaw(0x52),

    WalletPublicKey(0x60),
    Signature(0x61),
    RemainingSignatures(0x62),
    SignedHashes(0x63),

    Firmware(0x80),
    Batch(0x81),
    ManufactureDateTime(0x82),
    IssuerId(0x83),
    BlockchainId(0x84),
    ManufacturerPublicKey(0x85),
    CardIdManufacturerSignature(0x86),

    ProductMask(0x8A),
    PaymentFlowVersion(0x54),

    TokenSymbol(0xA0),
    TokenContractAddress(0xA1),
    TokenDecimal(0xA2),
    Denomination(0xC0),
    ValidatedBalance(0xC1),
    LastSignDate(0xC2),
    DenominationText(0xC3),

    TerminalIsLinked(0x58),
    TerminalPublicKey(0x5C),
    TerminalTransactionSignature(0x57),

    UserData(0x2A),
    UserProtectedData(0x2B),
    UserCounter(0x2C),
    UserProtectedCounter(0x2D);

    /**
     * @return [TlvValueType] associated with a [TlvTag]
     */
    fun valueType(): TlvValueType {
        return when (this) {
            CardId, Batch, CrExKey -> TlvValueType.HexString
            NewPin, NewPin2, NewPin3 -> TlvValueType.HexStringToHash
            ManufactureId, Firmware, IssuerId, BlockchainId, TokenSymbol, TokenContractAddress ->
                TlvValueType.Utf8String
            CurveId -> TlvValueType.EllipticCurve
            PauseBeforePin2, RemainingSignatures, SignedHashes, Health, TokenDecimal,
            Offset, Size -> TlvValueType.Uint16
            MaxSignatures, UserCounter, UserProtectedCounter, IssuerDataCounter -> TlvValueType.Uint32
            IsActivated, TerminalIsLinked, CreateWalletAtPersonalize -> TlvValueType.BoolValue
            ManufactureDateTime -> TlvValueType.DateTime
            ProductMask -> TlvValueType.ProductMask
            SettingsMask -> TlvValueType.SettingsMask
            Status -> TlvValueType.CardStatus
            SigningMethod -> TlvValueType.SigningMethod
            Mode -> TlvValueType.IssuerDataMode
            else -> TlvValueType.ByteArray
        }
    }

    companion object {
        private val values = values()
        fun byCode(code: Int): TlvTag = values.find { it.code == code } ?: Unknown
    }
}

