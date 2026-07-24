package com.tangem.data.staking.verification

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.NetworkType
import org.junit.jupiter.api.Test

/**
 * The exhaustive per-network parsing is covered by the SDK
 * (com.tangem.blockchain.transaction.staking.StakingTransactionRecognizerTest). Here we only verify
 * that the adapter routes each supported [NetworkType] to the SDK and reports unsupported ones as
 * not recognized.
 */
internal class DefaultStakingTransactionRecognizerTest {

    private val recognizer = DefaultStakingTransactionRecognizer()

    @Test
    fun `GIVEN tron staking tx WHEN recognize THEN delegates to true`() {
        val json = """{"raw_data":{"contract":[{"type":"FreezeBalanceV2Contract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.TRON, json)).isTrue()
    }

    @Test
    fun `GIVEN cosmos staking tx WHEN recognize THEN delegates to true`() {
        val hex = "0a232f636f736d6f732e7374616b696e672e763162657461312e4d736744656c6567617465"
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.COSMOS, hex)).isTrue()
    }

    @Test
    fun `GIVEN cardano staking tx WHEN recognize THEN delegates to true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.CARDANO, "81a10480")).isTrue()
    }

    @Test
    fun `GIVEN solana staking tx WHEN recognize THEN delegates to true`() {
        val hex = "0102" + "06a1d8179137542a983437bdfe2a7ab2557f535c8a78722b68a49dc000000000" + "0304"
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.SOLANA, hex)).isTrue()
    }

    @Test
    fun `GIVEN polygon staking tx WHEN recognize THEN delegates to true`() {
        val json = """{"to":"0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908","data":"0xe4457a8a"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, json)).isTrue()
    }

    @Test
    fun `GIVEN bsc staking tx WHEN recognize THEN delegates to true`() {
        val json = """{"to":"0x0000000000000000000000000000000000002002"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.BINANCE, json)).isTrue()
    }

    @Test
    fun `GIVEN supported network non-staking tx WHEN recognize THEN delegates to false`() {
        val json = """{"raw_data":{"contract":[{"type":"TriggerSmartContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.TRON, json)).isFalse()
    }

    @Test
    fun `GIVEN unsupported network WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.ETHEREUM, "deadbeef")).isFalse()
    }
}