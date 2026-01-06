package com.tangem.domain.transaction

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessSignedTransactionResult
import com.tangem.domain.transaction.models.GaslessTransactionData
import java.math.BigInteger

interface GaslessTransactionRepository {

    fun isNetworkSupported(network: Network): Boolean

    suspend fun getSupportedTokens(network: Network): Set<CryptoCurrency>

    fun getTokenFeeReceiverAddress(): String

    /**
     * Sends gasless transaction to the gasless service for signing and returns the signed result.
     *
     * Flow:
     * 1. User signs the gasless transaction data locally with their private key
     * 2. This method submits the transaction + user signature to the gasless service
     * 3. Service validates the signature and transaction data
     * 4. Service adds its own signature for fee delegation (pays gas in tokens)
     * 5. Service constructs the final EIP-1559 transaction with all signatures
     * 6. Service returns the fully signed transaction ready for broadcasting
     *
     * The gasless service acts as a relayer that:
     * - Pays network gas fees on behalf of the user
     * - Receives payment in the specified token from the user's wallet
     * - Ensures atomic execution (either both transfers succeed or both fail)
     *
     * @param gaslessTransactionData domain model containing:
     *                               - transaction: target contract call data (to, value, data)
     *                               - fee: token payment configuration (feeToken, maxTokenFee, etc.)
     *                               - nonce: user's contract nonce to prevent replay attacks
     * @param signature user's ECDSA signature of the gasless transaction in hex format (0x...)
     *                  Signs keccak256 hash of the transaction data
     * @param userAddress user's Ethereum address (EOA or contract wallet)
     * @param network blockchain network (Ethereum, Polygon, BSC, etc.)
     *                Used to determine chainId for the request
     * @param eip7702Auth optional EIP-7702 authorization for EOA delegation to smart contract
     *                    Required only when user's EOA needs to temporarily act as contract wallet
     *                    Contains signature authorizing delegation to entry point contract
     * @return [GaslessSignedTransactionResult] containing:
     *         - signedTransaction: complete RLP-encoded transaction ready to broadcast
     *         - gasLimit: actual gas limit allocated by the service
     *         - maxFeePerGas: maximum fee per gas (base + priority) in wei
     *         - maxPriorityFeePerGas: tip for validators in wei (EIP-1559)
     * @throws IllegalStateException if network is not supported or chainId cannot be determined
     * @throws Exception if service returns error or network request fails
     */
    suspend fun sendGaslessTransaction(
        gaslessTransactionData: GaslessTransactionData,
        signature: String,
        userAddress: String,
        network: Network,
        eip7702Auth: Eip7702Authorization? = null,
    ): GaslessSignedTransactionResult

    /**
     * Hardcoded value as baseGas
     */
    fun getBaseGasForTransaction(): BigInteger
}