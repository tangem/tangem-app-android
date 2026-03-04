package com.tangem.domain.pay.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.pay.PaymentAccountStatus

@Suppress("UnnecessaryAbstractClass")
abstract class PaymentAccountStatusSupplier(
    override val factory: PaymentAccountStatusProducer.Factory,
    override val keyCreator: (PaymentAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<PaymentAccountStatusProducer, PaymentAccountStatusProducer.Params, PaymentAccountStatus>()