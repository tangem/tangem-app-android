package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class AllowPermissionsHandlerImplTest {

    private val handler = AllowPermissionsHandlerImpl()

    @Test
    fun `GIVEN address added with amount WHEN queried THEN in progress and amount returned`() {
        handler.addAddressToInProgress(tokenAddress = "0xToken", approvedAmount = BigDecimal("5"))

        assertThat(handler.isAddressAllowanceInProgress("0xToken")).isTrue()
        assertThat(handler.getApprovedAmount("0xToken")).isEqualTo(BigDecimal("5"))
    }

    @Test
    fun `GIVEN address added without amount WHEN queried THEN in progress with null amount`() {
        handler.addAddressToInProgress(tokenAddress = "0xToken", approvedAmount = null)

        assertThat(handler.isAddressAllowanceInProgress("0xToken")).isTrue()
        assertThat(handler.getApprovedAmount("0xToken")).isNull()
    }

    @Test
    fun `GIVEN address removed WHEN queried THEN not in progress and amount is null`() {
        handler.addAddressToInProgress(tokenAddress = "0xToken", approvedAmount = BigDecimal("5"))

        handler.removeAddressFromProgress("0xToken")

        assertThat(handler.isAddressAllowanceInProgress("0xToken")).isFalse()
        assertThat(handler.getApprovedAmount("0xToken")).isNull()
    }

    @Test
    fun `GIVEN address never added WHEN queried THEN not in progress`() {
        assertThat(handler.isAddressAllowanceInProgress("0xUnknown")).isFalse()
        assertThat(handler.getApprovedAmount("0xUnknown")).isNull()
    }
}