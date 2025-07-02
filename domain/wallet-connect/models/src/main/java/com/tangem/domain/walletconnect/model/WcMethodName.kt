package com.tangem.domain.walletconnect.model

sealed interface WcMethodName {
    val raw: String

    data class Unsupported(override val raw: String) : WcMethodName
}

enum class WcEthMethodName(override val raw: String) : WcMethodName {
    EthSign("eth_sign"),
    PersonalSign("personal_sign"),
    SignTypeData("eth_signTypedData"),
    SignTypeDataV4("eth_signTypedData_v4"),
    SignTransaction("eth_signTransaction"),
    SendTransaction("eth_sendTransaction"),
    AddEthereumChain("wallet_addEthereumChain"),
}

enum class WcSolanaMethodName(override val raw: String) : WcMethodName {
    SignMessage("solana_signMessage"),
    SignTransaction("solana_signTransaction"),
    SendAllTransaction("solana_signAllTransactions"),
}