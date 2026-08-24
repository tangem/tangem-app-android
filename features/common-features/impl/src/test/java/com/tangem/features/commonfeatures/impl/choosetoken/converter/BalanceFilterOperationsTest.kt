package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.EmptyReason
import org.junit.jupiter.api.Test

internal class BalanceFilterOperationsTest {

    @Test
    fun `GIVEN HideZero not searching with available tokens WHEN resolveEmptyReason THEN FilteredOut`() {
        val reason = resolveEmptyReason(
            balanceFilter = BalanceFilter.HideZero,
            isSearching = false,
            hasAvailableTokens = true,
        )
        assertThat(reason).isEqualTo(EmptyReason.FilteredOut)
    }

    @Test
    fun `GIVEN HideZero but no available tokens WHEN resolveEmptyReason THEN NoTokens`() {
        val reason = resolveEmptyReason(
            balanceFilter = BalanceFilter.HideZero,
            isSearching = false,
            hasAvailableTokens = false,
        )
        assertThat(reason).isEqualTo(EmptyReason.NoTokens)
    }

    @Test
    fun `GIVEN searching WHEN resolveEmptyReason THEN NoTokens`() {
        val reason = resolveEmptyReason(
            balanceFilter = BalanceFilter.HideZero,
            isSearching = true,
            hasAvailableTokens = true,
        )
        assertThat(reason).isEqualTo(EmptyReason.NoTokens)
    }

    @Test
    fun `GIVEN All filter WHEN resolveEmptyReason THEN NoTokens`() {
        val reason = resolveEmptyReason(
            balanceFilter = BalanceFilter.All,
            isSearching = false,
            hasAvailableTokens = true,
        )
        assertThat(reason).isEqualTo(EmptyReason.NoTokens)
    }
}