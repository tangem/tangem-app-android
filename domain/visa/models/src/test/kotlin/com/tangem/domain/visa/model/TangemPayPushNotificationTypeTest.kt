package com.tangem.domain.visa.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.visa.model.TangemPayPushNotificationType.Action
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayPushNotificationTypeTest {

    @ParameterizedTest
    @MethodSource("provideTypes")
    fun `GIVEN backend type value WHEN fromValue THEN resolves to the expected action`(case: TypeCase) {
        // GIVEN
        val type = TangemPayPushNotificationType.fromValue(case.value)

        // THEN
        assertThat(type).isNotNull()
        assertThat(type?.action()).isEqualTo(case.action)
    }

    @Test
    fun `GIVEN all types WHEN reading values THEN every value is unique`() {
        // GIVEN
        val typesCount = TangemPayPushNotificationType.entries.size

        // THEN
        assertThat(TangemPayPushNotificationType.all).hasSize(typesCount)
    }

    @Test
    fun `GIVEN all types WHEN reading values THEN the whole set is covered by the test cases`() {
        // GIVEN
        val covered = provideTypes().map { it.value }.toSet()

        // THEN
        assertThat(covered).isEqualTo(TangemPayPushNotificationType.all)
    }

    @Test
    fun `GIVEN unknown type WHEN fromValue THEN returns null`() {
        // GIVEN
        val value = TangemPayPushNotificationType.fromValue("declined_reason18")

        // THEN
        assertThat(value).isNull()
    }

    internal data class TypeCase(val value: String, val action: Action) {
        override fun toString(): String = "$value -> $action"
    }

    private fun provideTypes() = listOf(
        TypeCase("card_ready", Action.CARD_DETAILS),
        TypeCase("transaction_spend", Action.SPEND_DETAILS),
        TypeCase("transaction_spend_refund", Action.SPEND_DETAILS),
        TypeCase("declined_top_up", Action.SPEND_DETAILS),
        TypeCase("declined_reason1", Action.SPEND_DETAILS),
        TypeCase("declined_reason2", Action.SPEND_DETAILS),
        TypeCase("declined_reason3", Action.SPEND_DETAILS),
        TypeCase("declined_reason4", Action.SPEND_DETAILS),
        TypeCase("declined_reason5", Action.SPEND_DETAILS),
        TypeCase("declined_reason6", Action.SPEND_DETAILS),
        TypeCase("declined_reason7", Action.SPEND_DETAILS),
        TypeCase("declined_reason8", Action.SPEND_DETAILS),
        TypeCase("declined_reason9", Action.SPEND_DETAILS),
        TypeCase("declined_reason10", Action.SPEND_DETAILS),
        TypeCase("declined_reason11", Action.SPEND_DETAILS),
        TypeCase("declined_reason12", Action.SPEND_DETAILS),
        TypeCase("declined_reason13", Action.SPEND_DETAILS),
        TypeCase("declined_reason14", Action.SPEND_DETAILS),
        TypeCase("declined_reason15", Action.SPEND_DETAILS),
        TypeCase("declined_reason16", Action.SPEND_DETAILS),
        TypeCase("declined_reason17", Action.SPEND_DETAILS),
        TypeCase("collateral_deposit", Action.COLLATERAL_DETAILS),
        TypeCase("collateral_withdraw", Action.COLLATERAL_DETAILS),
        TypeCase("threshold1_top_up", Action.TOP_UP),
    )
}