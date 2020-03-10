package com.tangem.blockchain.xrp.network.rippled

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.retryIO
import com.tangem.blockchain.xrp.network.XrpFeeResponse
import com.tangem.blockchain.xrp.network.XrpInfoResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RippledProvider(private val api: RippledApi) {
    private val decimals = Blockchain.XRP.decimals.toInt()

    suspend fun getInfo(address: String): Result<XrpInfoResponse> {
        return try {
            coroutineScope {
                val accountBody = makeAccountBody(address, validated = true)
                val accountDeferred = retryIO { async { api.getAccount(accountBody) } }

                val unconfirmedBody = makeAccountBody(address, validated = false)
                val unconfirmedDeferred = retryIO { async { api.getAccount(unconfirmedBody) } }

                val stateDeferred = retryIO { async { api.getServerState() } }

                val accountData = accountDeferred.await()
                val unconfirmedData = unconfirmedDeferred.await()
                val serverState = stateDeferred.await()

                val reserveBase = serverState.result!!.state!!.validatedLedger!!.reserveBase!!
                        .toBigDecimal().movePointLeft(decimals)

                if (accountData.result!!.errorCode == 19) {
                    Result.Success(XrpInfoResponse(
                            reserveBase = reserveBase,
                            accountFound = false
                    ))
                } else {
                    val confirmedBalance =
                            accountData.result!!.accountData!!.balance!!.toBigDecimal()
                                    .movePointLeft(decimals)
                    val unconfirmedBalance =
                            unconfirmedData.result!!.accountData!!.balance!!.toBigDecimal()
                                    .movePointLeft(decimals)

                    Result.Success(XrpInfoResponse(
                            balance = confirmedBalance,
                            sequence = accountData.result!!.accountData!!.sequence!!,
                            hasUnconfirmed = confirmedBalance != unconfirmedBalance,
                            reserveBase = reserveBase
                    ))
                }

            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun getFee(): Result<XrpFeeResponse> {
        return try {
            val feeData = retryIO { api.getFee() }
            Result.Success(XrpFeeResponse(
                    feeData.result!!.feeData!!.minimalFee!!.toBigDecimal().movePointLeft(decimals),
                    feeData.result!!.feeData!!.normalFee!!.toBigDecimal().movePointLeft(decimals),
                    feeData.result!!.feeData!!.priorityFee!!.toBigDecimal().movePointLeft(decimals)
            ))
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val submitBody = makeSubmitBody(transaction)
            val submitData = retryIO { api.submitTransaction(submitBody) }
            if (submitData.result!!.resultCode == 0) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(Exception(submitData.result!!.resultMessage
                        ?: submitData.result!!.errorException))
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }
}

private fun makeAccountBody(address: String, validated: Boolean): RippledBody {
    val params = HashMap<String, String>()
    params["account"] = address
    params["ledger_index"] = if (validated) "validated" else "current"
    return RippledBody(RippledMethod.ACCOUNT_INFO.value, params)
}

private fun makeSubmitBody(transaction: String): RippledBody {
    val params = HashMap<String, String>()
    params["tx_blob"] = transaction
    return RippledBody(RippledMethod.SUBMIT.value, params)
}