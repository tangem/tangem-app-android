package com.tangem.tangemtest._arch.structure

/**
[REDACTED_AUTHOR]
 */
interface Id

enum class BlockId : Id {
    CARD_NUMBER,
    COMMON,
    SIGNING_METHOD,
    SIGN_HASH_EX_PROP,
    DENOMINATION,
    TOKEN,
    PROD_MASK,
    SETTINGS_MASK,
    SETTINGS_MASK_PROTOCOL_ENC,
    SETTINGS_MASK_NDEF,
    PINS,
}

enum class CardNumber : Id {
    SERIES,
    NUMBER,
}

enum class Common : Id {
    CURVE,
    BLOCKCHAIN,
    BLOCKCHAIN_CUSTOM,
    MAX_SIGNATURES,
    CREATE_WALLET,
}

enum class SigningMethod : Id {
    SIGN_TX,
    SIGN_TX_RAW,
    SIGN_VALIDATED_TX,
    SIGN_VALIDATED_TX_RAW,
    SIGN_VALIDATED_TX_ISSUER,
    SIGN_VALIDATED_TX_RAW_ISSUER,
    SIGN_EXTERNAL,
}

enum class SignHashExProp : Id {
    PIN_LESS_FLOOR_LIMIT,
    CRYPTO_EXTRACT_KEY,
    REQUIRE_TERMINAL_CERT_SIG,
    REQUIRE_TERMINAL_TX_SIG,
    CHECK_PIN3,
}

enum class Denomination : Id {
    WRITE_ON_PERSONALIZE,
    DENOMINATION,
}

enum class Token : Id {
    ITS_TOKEN,
    SYMBOL,
    CONTRACT_ADDRESS,
    DECIMAL
}

enum class ProductMask : Id {
    NOTE,
    TAG,
    ID_CARD
}

enum class SettingsMask : Id {
    IS_REUSABLE,
    NEED_ACTIVATION,
    FORBID_PURGE,
    ALLOW_SELECT_BLOCKCHAIN,
    USE_BLOCK,
    ONE_APDU,
    USE_CVC,
    ALLOW_SWAP_PIN,
    ALLOW_SWAP_PIN2,
    FORBID_DEFAULT_PIN,
    SMART_SECURITY_DELAY,
    PROTECT_ISSUER_DATA_AGAINST_REPLAY,
    SKIP_SECURITY_DELAY_IF_VALIDATED,
    SKIP_PIN2_CVC_IF_VALIDATED,
    SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL,
    RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA,
}

enum class SettingsMaskProtocolEnc : Id {
    ALLOW_UNENCRYPTED,
    ALLOW_FAST_ENCRYPTION
}

enum class SettingsMaskNdef : Id {
    USE_NDEF,
    DYNAMIC_NDEF,
    DISABLE_PRECOMPUTED_NDEF,
    AAR,
    CUSTOM_AAR
}

enum class Pins : Id {
    PIN,
    PIN2,
    PIN3,
    CVC,
    PAUSE_BEFORE_PIN2
}

enum class Additional : Id {
    UNDEFINED,
    JSON_INCOMING,
    JSON_OUTGOING,
    JSON_TAILS,
}