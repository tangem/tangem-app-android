package com.tangem.common.routing.deeplink

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampDeeplinkTargetTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN onramp deeplink query WHEN resolved THEN target follows tx id, result, type priority`(
        model: ResolveModel,
    ) {
        // Act
        val actual = resolveOnrampDeeplinkTarget(model.queryParams)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ResolveModel(
        val queryParams: Map<String, String>,
        val expected: OnrampDeeplinkTarget,
    )

    private fun provideTestModels() = listOf(
        ResolveModel(mapOf("tx_id" to "tx"), OnrampDeeplinkTarget.Transaction("tx")),
        ResolveModel(
            mapOf("tx_id" to "tx", "result" to "success", "type" to "concierge_waitlist"),
            OnrampDeeplinkTarget.Transaction("tx"),
        ),
        ResolveModel(mapOf("result" to "success"), OnrampDeeplinkTarget.Redirect(isSuccess = true)),
        ResolveModel(mapOf("result" to "cancel"), OnrampDeeplinkTarget.Redirect(isSuccess = false)),
        ResolveModel(
            mapOf("result" to "cancel", "type" to "concierge_waitlist"),
            OnrampDeeplinkTarget.Redirect(isSuccess = false),
        ),
        ResolveModel(mapOf("type" to "concierge_waitlist"), OnrampDeeplinkTarget.ConciergeWaitlist),
        ResolveModel(
            mapOf("tx_id" to "", "result" to "other", "type" to "concierge_waitlist"),
            OnrampDeeplinkTarget.ConciergeWaitlist,
        ),
        ResolveModel(mapOf("type" to "concierge_waitlist_v2"), OnrampDeeplinkTarget.Invalid),
        ResolveModel(mapOf("result" to "other"), OnrampDeeplinkTarget.Invalid),
        ResolveModel(emptyMap(), OnrampDeeplinkTarget.Invalid),
    )
}