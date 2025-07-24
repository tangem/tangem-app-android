package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.flow.Flow

interface WcMethodUseCase

interface WcMethodContext {
    val session: WcSession
    val rawSdkRequest: WcSdkSessionRequest
    val network: Network
    val method: WcMethod
    val derivationState: WcNetworkDerivationState
    val wallet: UserWallet get() = session.wallet
}

sealed class WcNetworkDerivationState {
    data object Single : WcNetworkDerivationState()
    data class Multiple(val walletAddress: String) : WcNetworkDerivationState()
}

/**
 * Wc Methods that must be signed by Tangem card
 * [WcSignState] represents signing state
 */
interface WcSignUseCase<SignModel> : WcMethodContext, WcMethodUseCase {

    operator fun invoke(): Flow<WcSignState<SignModel>>
    fun cancel()
    fun sign()
}

data class WcSignState<SignModel>(
    val signModel: SignModel,
    val domainStep: WcSignStep,
)

sealed interface WcSignStep {
    data object PreSign : WcSignStep
    data object Signing : WcSignStep
    data class Result(val result: Either<WcRequestError, String>) : WcSignStep
}