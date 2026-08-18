package com.tangem.data.cloudbackup.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.data.cloudbackup.CloudBackupJson
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.ByteArrayInputStream

/**
 * Guards the cross-platform payload format (UC-07 §4.1). A change here silently breaks restoring

 */
@OptIn(ExperimentalSerializationApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CloudBackupSecretTest {

    @Test
    fun `GIVEN wallet without passphrase WHEN encoded THEN passphraseRequired is zero`() {
        val actual = CloudBackupSecret.encode(mnemonic = MNEMONIC.toCharArray(), isPassphraseRequired = false)

        assertThat(actual?.decodeToString())
            .isEqualTo("""{"mnemonic":"word1 word2 word3","passphraseRequired":0}""")
    }

    @Test
    fun `GIVEN wallet with passphrase WHEN encoded THEN passphraseRequired is one`() {
        val actual = CloudBackupSecret.encode(mnemonic = MNEMONIC.toCharArray(), isPassphraseRequired = true)

        assertThat(actual?.decodeToString())
            .isEqualTo("""{"mnemonic":"word1 word2 word3","passphraseRequired":1}""")
    }

    @Test
    fun `GIVEN a non-ASCII mnemonic WHEN encoded THEN the words survive the UTF-8 round trip`() {
        val japanese = "あいこくしん あいさつ"

        val actual = CloudBackupSecret.encode(mnemonic = japanese.toCharArray(), isPassphraseRequired = false)

        assertThat(actual?.decodeToString())
            .isEqualTo("""{"mnemonic":"$japanese","passphraseRequired":0}""")
    }

    @Test
    fun `GIVEN a mnemonic that JSON would escape WHEN encoded THEN nothing is produced`() {
        val actual = CloudBackupSecret.encode(mnemonic = """word" \ word""".toCharArray(), isPassphraseRequired = false)

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN encoded payload WHEN decoded THEN the mnemonic is returned as a wipeable array`() {
        val encoded = CloudBackupSecret.encode(mnemonic = MNEMONIC.toCharArray(), isPassphraseRequired = true)!!

        val actual = CloudBackupJson.decodeFromStream<CloudBackupSecret>(ByteArrayInputStream(encoded))

        assertThat(actual.mnemonic).isEqualTo(MNEMONIC.toCharArray())
        assertThat(actual.isPassphraseRequired).isTrue()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun decode(model: DecodeModel) {
        val actual = runCatching {
            CloudBackupJson.decodeFromStream<CloudBackupSecret>(ByteArrayInputStream(model.payload.toByteArray()))
        }.getOrNull()

        assertThat(actual?.mnemonic).isEqualTo(model.expectedMnemonic?.toCharArray())
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
        // the fields may arrive in any order
        DecodeModel(
            payload = """{"passphraseRequired":1,"mnemonic":"word1 word2"}""",
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

    private companion object {
        const val MNEMONIC = "word1 word2 word3"
    }
}