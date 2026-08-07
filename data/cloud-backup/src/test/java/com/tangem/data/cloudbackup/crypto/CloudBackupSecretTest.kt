package com.tangem.data.cloudbackup.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.data.cloudbackup.CloudBackupJson
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the cross-platform payload format (UC-07 §4.1). A change here silently breaks restoring

 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CloudBackupSecretTest {

    @Test
    fun `GIVEN wallet without passphrase WHEN encoded THEN passphraseRequired is zero`() {
        val payload = CloudBackupSecret.of(mnemonic = "word1 word2 word3", isPassphraseRequired = false)

        val actual = CloudBackupJson.encodeToString(payload)

        assertThat(actual).isEqualTo("""{"mnemonic":"word1 word2 word3","passphraseRequired":0}""")
    }

    @Test
    fun `GIVEN wallet with passphrase WHEN encoded THEN passphraseRequired is one`() {
        val payload = CloudBackupSecret.of(mnemonic = "word1 word2 word3", isPassphraseRequired = true)

        val actual = CloudBackupJson.encodeToString(payload)

        assertThat(actual).isEqualTo("""{"mnemonic":"word1 word2 word3","passphraseRequired":1}""")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun decode(model: DecodeModel) {
        val actual = runCatching {
            CloudBackupJson.decodeFromString<CloudBackupSecret>(model.payload)
        }.getOrNull()

        assertThat(actual?.mnemonic).isEqualTo(model.expectedMnemonic)
        assertThat(actual?.isPassphraseRequired).isEqualTo(model.expectedPassphraseRequired)
    }

    internal data class DecodeModel(
        val payload: String,
        val expectedMnemonic: String?,
        val expectedPassphraseRequired: Boolean?,
    )

    private fun provideTestModels() = listOf(
        DecodeModel(
            payload = """{"mnemonic":"word1 word2","passphraseRequired":0}""",
            expectedMnemonic = "word1 word2",
            expectedPassphraseRequired = false,
        ),
        DecodeModel(
            payload = """{"mnemonic":"word1 word2","passphraseRequired":1}""",
            expectedMnemonic = "word1 word2",
            expectedPassphraseRequired = true,
        ),
        // unknown keys are tolerated so a newer iOS payload still restores
        DecodeModel(
            payload = """{"mnemonic":"word1 word2","passphraseRequired":1,"future":"x"}""",
            expectedMnemonic = "word1 word2",
            expectedPassphraseRequired = true,
        ),
        // passphraseRequired is mandatory — the legacy payload without it must not parse
        DecodeModel(payload = """{"mnemonic":"word1 word2"}""", expectedMnemonic = null, expectedPassphraseRequired = null),
        // out-of-contract flag values parse but resolve to no answer, so the caller rejects the file
        DecodeModel(
            payload = """{"mnemonic":"word1 word2","passphraseRequired":2}""",
            expectedMnemonic = "word1 word2",
            expectedPassphraseRequired = null,
        ),
        DecodeModel(payload = """{"passphraseRequired":0}""", expectedMnemonic = null, expectedPassphraseRequired = null),
        DecodeModel(payload = "word1 word2 0", expectedMnemonic = null, expectedPassphraseRequired = null),
        DecodeModel(payload = "", expectedMnemonic = null, expectedPassphraseRequired = null),
    )
}