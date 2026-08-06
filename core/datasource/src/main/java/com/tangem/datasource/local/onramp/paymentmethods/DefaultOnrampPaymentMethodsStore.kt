package com.tangem.datasource.local.onramp.paymentmethods

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO

internal class DefaultOnrampPaymentMethodsStore(
    store: RuntimeSharedMapStore<String, List<PaymentMethodDTO>>,
) : OnrampPaymentMethodsStore, RuntimeSharedMapStore<String, List<PaymentMethodDTO>> by store