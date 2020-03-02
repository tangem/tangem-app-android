package com.tangem.blockchain.stellar

import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.network.API_STELLAR
import com.tangem.blockchain.common.network.API_STELLAR_TESTNET
import com.tangem.blockchain.stellar.StellarWalletManager.Companion.STROOPS_IN_XLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.requests.ErrorResponse
import java.io.IOException
import java.math.BigDecimal

class StellarNetworkManager(isTestNet: Boolean) {

    val network: Network = if (isTestNet) Network.TESTNET else Network.PUBLIC
    private val stellarServer by lazy {
        Server(if (isTestNet) API_STELLAR_TESTNET else API_STELLAR)
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = stellarServer.submitTransaction(Transaction.fromEnvelopeXdr(transaction, network))
            if (response.isSuccess) {
                SimpleResult.Success
            } else {
                val trResult: String? = response.extras?.resultCodes?.transactionResultCode +
                        (response.extras?.resultCodes?.operationsResultCodes?.getOrNull(0) ?: "")
                SimpleResult.Failure(Exception(trResult ?: "transaction failed"))
            }
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    suspend fun checkIsAccountCreated(address: String): Boolean {
        try {
            stellarServer.accounts().account(address)
            return true
        } catch (errorResponse: ErrorResponse) {
            if (errorResponse.code == 404) return false
            return false
        } catch (exception: IOException) {
            return false
        }
    }

    suspend fun getInfo(accountId: String, assetCode: String? = null): Result<StellarResponse> {
        return try {
            coroutineScope {
                val accountResponseDefered = async(Dispatchers.IO) { stellarServer.accounts().account(accountId) }
                val ledgerResponseDeferred = async(Dispatchers.IO) {
                    val latestLedger: Int = stellarServer.root().historyLatestLedger
                    stellarServer.ledgers().ledger(latestLedger.toLong())
                }

                val accountResponse = accountResponseDefered.await()
                val balance = accountResponse.balances
                        .find { it.assetType == "native" }
                        ?.balance?.toBigDecimal()
                        ?: return@coroutineScope Result.Failure(Exception("Stellar Balance not found"))
                val assetBalance = if (assetCode == null) {
                    null
                } else {
                    accountResponse.balances
                            .find { it.assetType != "native" && it.assetIssuer == assetCode }
                            ?.balance?.toBigDecimal()
                            ?: return@coroutineScope Result.Failure(Exception("Stellar Balance not found"))
                }
                val sequence = accountResponse.sequenceNumber

                val ledgerResponse = ledgerResponseDeferred.await()
                val baseFee = ledgerResponse.baseFeeInStroops.toBigDecimal().divide(STROOPS_IN_XLM)
                val baseReserve = ledgerResponse.baseReserveInStroops.toBigDecimal().divide(STROOPS_IN_XLM)

                Result.Success(StellarResponse(
                        baseFee,
                        baseReserve,
                        assetBalance,
                        balance,
                        sequence
                ))
            }
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }
}

data class StellarResponse(
        val baseFee: BigDecimal,
        val baseReserve: BigDecimal,
        val assetBalance: BigDecimal?,
        val balance: BigDecimal,
        val sequence: Long
)