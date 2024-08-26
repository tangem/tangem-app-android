package com.tangem.feature.wallet.presentation.wallet.domain

import com.google.common.truth.Truth
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class Wallet2CobrandImageTest {

    @Test
    fun checkUniqueness() {
        val excluded = listOf(Wallet2CobrandImage.Pastel, Wallet2CobrandImage.Vivid)
        (Wallet2CobrandImage.entries - excluded).forEach {
            Truth.assertThat(it.cards2ResId != it.cards3ResId).isTrue()
        }

        Truth.assertThat(Wallet2CobrandImage.entries.map { it.cards2ResId }).containsNoDuplicates()
        Truth.assertThat(Wallet2CobrandImage.entries.map { it.cards3ResId }).containsNoDuplicates()
        Truth.assertThat(Wallet2CobrandImage.entries.flatMap { it.batchIds }).containsNoDuplicates()
    }
}