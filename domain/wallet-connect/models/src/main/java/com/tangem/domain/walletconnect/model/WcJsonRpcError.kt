package com.tangem.domain.walletconnect.model

/**
 * The errors the app answers a dApp with, from the EIP-1193 provider range and the JSON-RPC 2.0 reserved
 * range.
 *
 * A dApp branches on the code: [UnrecognizedChain] in particular is what makes it follow a refused
 * `wallet_switchEthereumChain` with `wallet_addEthereumChain`, so answering everything with `0` left the
 * dApp unable to tell a missing network from a user refusal.
 */
enum class WcJsonRpcError(val code: Int, val message: String) {
    UserRejected(code = 4001, message = "User rejected the request."),
    UnsupportedMethod(code = 4200, message = "The requested method is not supported."),
    UnrecognizedChain(code = 4902, message = "Unrecognized chain ID."),
    InvalidParams(code = -32602, message = "Invalid params."),
}