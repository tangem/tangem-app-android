package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

interface WcMethodUseCase : WcMethodContext

interface WcMethodContext {
    val session: WcSession
    val rawSdkRequest: WcSdkSessionRequest
    val network: Network
    val method: WcMethod
    val walletAddress: String
    val wallet: UserWallet get() = session.wallet
}

/**
 * Wc Methods that support an updatable fee
 */
interface WcMutableFee {
    fun updateFee(fee: Fee)
}

/**
 * Wc Methods that must be signed by Tangem card
 * [WcSignState] represents signing state
 */
interface WcSignUseCase : WcMethodContext {

    fun cancel()
    fun sign()

    interface SimpleRun<SignModel> {
        operator fun invoke(): Flow<WcSignState<SignModel>>
    }

    interface ArgsRun<SignModel, Args> {
        operator fun invoke(args: Args): Flow<WcSignState<SignModel>>
    }
}

data class WcSignState<SignModel>(
    val signModel: SignModel,
    val domainStep: WcSignStep,
)

sealed interface WcSignStep {
    data object PreSign : WcSignStep
    data object Signing : WcSignStep
    data class Result(val result: Either<Throwable, Unit>) : WcSignStep
}