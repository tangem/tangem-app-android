package com.tangem.domain.polymarket.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Producer of the prediction account status of a single wallet.
 *
 * It produces the status *value*, not the account status: the account type is owned by the shared account model
 * and wrapping the value into it is the job of whoever assembles the wallet's account list. Keeping the producer
 * on the value means it can be built and tested without that type.
 */
interface PredictionAccountStatusProducer : FlowProducer<PredictionAccountStatusValue> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, PredictionAccountStatusProducer>
}