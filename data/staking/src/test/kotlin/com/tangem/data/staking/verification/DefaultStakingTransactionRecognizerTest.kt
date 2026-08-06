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
        val json = """{"raw_data_hex":"$TRON_FREEZE_BALANCE_V2_RAW_DATA_HEX"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.TRON, json)).isTrue()
    }

    @Test
    fun `GIVEN cosmos staking tx WHEN recognize THEN delegates to true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.COSMOS, COSMOS_DELEGATE_HEX)).isTrue()
    }

    @Test
    fun `GIVEN cardano staking tx WHEN recognize THEN delegates to true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.CARDANO, "81a10480")).isTrue()
    }

    @Test
    fun `GIVEN solana staking tx WHEN recognize THEN delegates to true`() {
        val signatureAndHeader = "01" + "00".repeat(SOLANA_SIGNATURE_BYTES) + "010001"
        val accountKeys = "11".repeat(SOLANA_ACCOUNT_KEY_BYTES) + SOLANA_STAKE_PROGRAM_KEY
        val hex = signatureAndHeader + "02" + accountKeys
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
        val json = """{"raw_data_hex":"$TRON_TRIGGER_SMART_CONTRACT_RAW_DATA_HEX"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.TRON, json)).isFalse()
    }

    @Test
    fun `GIVEN unsupported network WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(NetworkType.ETHEREUM, "deadbeef")).isFalse()
    }

    private companion object {

        // Tron `Transaction.raw` protobuf with a single contract: field 11 (contract) → field 1 (type).
        // 0x36 = FreezeBalanceV2Contract (54), 0x1f = TriggerSmartContract (31).
        const val TRON_FREEZE_BALANCE_V2_RAW_DATA_HEX = "5a020836"
        const val TRON_TRIGGER_SMART_CONTRACT_RAW_DATA_HEX = "5a02081f"

        // `CosmosProtoMessage` protobuf whose delegate messageType is "/cosmos.staking.v1beta1.MsgDelegate".
        const val COSMOS_DELEGATE_HEX = "0a400a270a232f636f736d6f732e7374616b696e672e763162657461312e4d7367" +
            "44656c656761746512001215766961205374616b654b6974204349442d3130303912440a2d0a250a1f2f636f736d6f" +
            "732e63727970746f2e736563703235366b312e5075624b657912020a0012040a02080112130a0d0a057561746f6d12" +
            "043635363510cd88281a0b636f736d6f736875622d342001"

        const val SOLANA_STAKE_PROGRAM_KEY = "06a1d8179137542a983437bdfe2a7ab2557f535c8a78722b68a49dc000000000"
        const val SOLANA_SIGNATURE_BYTES = 64
        const val SOLANA_ACCOUNT_KEY_BYTES = 32
    }
}