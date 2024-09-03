package com.tangem.feature.wallet.presentation.wallet.domain

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class NoteImageTest {

    @Test
    fun check() {
        // check uniqueness
        Truth.assertThat(NoteImage.entries.map { it.blockchain }).containsNoDuplicates()
        Truth.assertThat(NoteImage.entries.map { it.imageResId }).containsNoDuplicates()

        // check blockchain matching
        Truth.assertThat(NoteImage.Bitcoin.blockchain).isEqualTo(Blockchain.Bitcoin)
        Truth.assertThat(NoteImage.Ethereum.blockchain).isEqualTo(Blockchain.Ethereum)
        Truth.assertThat(NoteImage.Binance.blockchain).isEqualTo(Blockchain.BSC)
        Truth.assertThat(NoteImage.Dogecoin.blockchain).isEqualTo(Blockchain.Dogecoin)
        Truth.assertThat(NoteImage.Cardano.blockchain).isEqualTo(Blockchain.Cardano)
        Truth.assertThat(NoteImage.XRP.blockchain).isEqualTo(Blockchain.XRP)
    }
}