package com.tangem.core.analytics.paramsinterceptor

import com.google.common.truth.Truth
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendTransactionSignerInfoInterceptorTest {

    private lateinit var interceptor: SendTransactionSignerInfoInterceptor

    @BeforeEach
    fun setUp() {
        interceptor = SendTransactionSignerInfoInterceptor()
    }

    @Test
    fun `id returns stable identifier`() {
        Truth.assertThat(interceptor.id()).isEqualTo("SendTransactionSignerInfoInterceptor")
    }

    @Test
    fun `canBeAppliedTo returns true for TransactionSent event`() {
        val event = mockk<Basic.TransactionSent>()

        Truth.assertThat(interceptor.canBeAppliedTo(event)).isTrue()
    }

    @Test
    fun `canBeAppliedTo returns false for any other event`() {
        val event = mockk<AnalyticsEvent>()

        Truth.assertThat(interceptor.canBeAppliedTo(event)).isFalse()
    }

    @Test
    fun `intercept writes Card by default`() {
        val params = mutableMapOf<String, String>()

        interceptor.intercept(params)

        Truth.assertThat(params[AnalyticsParam.WALLET_FORM])
            .isEqualTo(Basic.TransactionSent.WalletForm.Card.name)
    }

    @Test
    fun `intercept writes last updated wallet form`() {
        interceptor.update(Basic.TransactionSent.WalletForm.Ring)
        val params = mutableMapOf<String, String>()

        interceptor.intercept(params)

        Truth.assertThat(params[AnalyticsParam.WALLET_FORM])
            .isEqualTo(Basic.TransactionSent.WalletForm.Ring.name)
    }

    @Test
    fun `update overrides previous wallet form`() {
        interceptor.update(Basic.TransactionSent.WalletForm.Ring)
        interceptor.update(Basic.TransactionSent.WalletForm.Card)
        val params = mutableMapOf<String, String>()

        interceptor.intercept(params)

        Truth.assertThat(params[AnalyticsParam.WALLET_FORM])
            .isEqualTo(Basic.TransactionSent.WalletForm.Card.name)
    }

    @Test
    fun `intercept preserves other params`() {
        val params = mutableMapOf("Source" to "Send")

        interceptor.intercept(params)

        Truth.assertThat(params).containsEntry("Source", "Send")
        Truth.assertThat(params).containsKey(AnalyticsParam.WALLET_FORM)
    }
}