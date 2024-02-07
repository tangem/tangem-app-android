package com.tangem.lib.visa.utils

internal object VisaConfig {

    const val BASE_RPC_URL = "https://rpc-mumbai.maticvigil.com/"
    const val BRIDGE_PROCESSOR_CONTRACT_ADDRESS = "0x62119697e78178512bfcc456ae6d1b7dee9fbaa6"
    const val CHAIN_ID = 80_001L
    const val DECIMALS = 9
    const val GAS_LIMIT = 500_000_000L
    const val PRIVATE_KEY_LENGTH = 32

    const val NETWORK_TIMEOUT_SECONDS = 65L
    const val NETWORK_LOGS_TAG = "VisaNetworkLogs"
}