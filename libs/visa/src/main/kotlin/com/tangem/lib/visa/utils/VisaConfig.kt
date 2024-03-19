package com.tangem.lib.visa.utils

internal object VisaConfig {

    const val BASE_RPC_URL = "https://polygon-mumbai.g.alchemy.com/v2/_1qqjXgBC_IikaXChnna8KTcV2eMMIQG/"
    const val BRIDGE_PROCESSOR_CONTRACT_ADDRESS = "0xe32ecbbc1ec17fa9c160569cd613ad568ca50279"
    const val PAYMENT_ACCOUNT_REGISTRY_ADDRESS = "0x3f4ae01073d1a9d5a92315fe118e57d1cdec7c44"
    const val CHAIN_ID = 80_001L
    const val DECIMALS = 9
    const val GAS_LIMIT = 500_000_000L
    const val PRIVATE_KEY_LENGTH = 32

    const val VISA_API_PROD_URL = "https://payapi.tangem-tech.com/api/v1/"
    const val VISA_API_DEV_URL = "https://devpayapi.tangem-tech.com/api/v1/"

    const val NETWORK_TIMEOUT_SECONDS = 65L
    const val NETWORK_LOGS_TAG = "VisaNetworkLogs"
}
