package com.tangem.core.analytics.paramsinterceptor

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.store.LastSignedWalletFormStore
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendTransactionSignerInfoInterceptor @Inject constructor() :
    ParamsInterceptor,
    LastSignedWalletFormStore {

    private val walletForm = MutableStateFlow(Basic.TransactionSent.WalletForm.Card)

    override fun update(form: Basic.TransactionSent.WalletForm) {
        walletForm.value = form
    }

    override fun id(): String = ID

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is Basic.TransactionSent

    override fun intercept(params: MutableMap<String, String>) {
        params[AnalyticsParam.WALLET_FORM] = walletForm.value.name
    }

    private companion object {
        const val ID = "SendTransactionSignerInfoInterceptor"
    }
}