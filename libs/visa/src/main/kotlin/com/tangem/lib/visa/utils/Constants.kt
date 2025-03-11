package com.tangem.lib.visa.utils

internal object Constants {

    const val MAINNET_RPC_URL = "https://polygon-rpc.com/"
    const val TESTNET_RPC_URL = "https://rpc-amoy.polygon.technology/"

    const val CHAIN_ID = 80_001L
    const val DECIMALS = 9
    const val GAS_LIMIT = 500_000_000L
    const val PRIVATE_KEY_LENGTH = 32

    const val VISA_API_PROD_URL = "https://payapi.tangem-tech.com/api/v1/"
    const val VISA_API_DEV_URL = "[REDACTED_ENV_URL]"

    const val NETWORK_TIMEOUT_SECONDS = 65L
    const val NETWORK_LOGS_TAG = "VisaNetworkLogs"
}