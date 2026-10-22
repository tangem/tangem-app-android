package com.tangem.data.staking.verification

import android.util.Base64
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonEncodingException
import com.tangem.domain.models.staking.NetworkType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultStakingBlockAidRequestFactoryTest {

    private val factory = DefaultStakingBlockAidRequestFactory()

    // blockchain-sdk's encodeBase64NoWrap() delegates to android.util.Base64, which throws
    // "not mocked" on the JVM. Delegate it to the real JDK encoder so the Solana path is testable.
    @BeforeEach
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `GIVEN polygon evm tx WHEN create THEN chain mapped and params wrapped in array`() {
        // Arrange
        val unsigned = """{"from":"0xaa","to":"0xbb","data":"0xe4457a8a"}"""

        // Act
        val result = factory.create(NetworkType.POLYGON, accountAddress = "0xaa", unsignedTransaction = unsigned)

        // Assert
        val expected = TransactionData(
            chain = "polygon",
            accountAddress = "0xaa",
            method = "eth_sendTransaction",
            domainUrl = "",
            params = TransactionParams.Evm(params = "[$unsigned]"),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN evm tx with extra fields WHEN create THEN params normalized to from-to-data-value`() {
        // Arrange
        val unsigned =
            """{"from":"0xaa","to":"0xbb","data":"0xcc","value":"0x1","gasLimit":"0x5208","nonce":3,"chainId":1,"type":2}"""

        // Act
        val result = factory.create(NetworkType.POLYGON, accountAddress = "0xaa", unsignedTransaction = unsigned)

        // Assert
        assertThat((result.params as TransactionParams.Evm).params)
            .isEqualTo("""[{"from":"0xaa","to":"0xbb","data":"0xcc","value":"0x1"}]""")
    }

    @Test
    fun `GIVEN bsc tx WHEN create THEN chain is bsc`() {
        // Act
        val result = factory.create(NetworkType.BINANCE, "0xaa", """{"from":"0xaa"}""")

        // Assert
        assertThat(result.chain).isEqualTo("bsc")
    }

    @Test
    fun `GIVEN ethereum tx WHEN create THEN chain is ethereum`() {
        // Act
        val result = factory.create(NetworkType.ETHEREUM, "0xaa", """{"from":"0xaa"}""")

        // Assert
        assertThat(result.chain).isEqualTo("ethereum")
    }

    @Test
    fun `GIVEN solana tx WHEN create THEN chain mainnet and hex converted to base64`() {
        // Arrange
        // hex "0102" -> bytes [1,2] -> base64 "AQI="
        // base58 "abc" -> bytes [0x01,0xB9,0x7B] -> base64 "Abl7"
        val accountAddress = "abc"
        val unsigned = "0102"

        // Act
        val result = factory.create(NetworkType.SOLANA, accountAddress = accountAddress, unsignedTransaction = unsigned)

        // Assert
        assertThat(result.chain).isEqualTo("mainnet")
        assertThat(result.method).isEqualTo("signTransaction")
        assertThat(result.accountAddress).isEqualTo("Abl7")
        val params = result.params as TransactionParams.Solana
        assertThat(params.transactions).containsExactly("AQI=")
    }

    @Test
    fun `GIVEN unsupported network WHEN create THEN throws`() {
        // Act
        val error = runCatching { factory.create(NetworkType.TON, "addr", "00") }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN evm payload is json null WHEN create THEN raw payload wrapped in array`() {
        // Act
        val result = factory.create(NetworkType.ETHEREUM, accountAddress = "0xaa", unsignedTransaction = "null")

        // Assert
        assertThat((result.params as TransactionParams.Evm).params).isEqualTo("[null]")
    }

    @Test
    fun `GIVEN evm payload is whitespace padded json null WHEN create THEN raw payload kept verbatim in array`() {
        // Act
        val result = factory.create(NetworkType.ETHEREUM, accountAddress = "0xaa", unsignedTransaction = " null ")

        // Assert
        assertThat((result.params as TransactionParams.Evm).params).isEqualTo("[ null ]")
    }

    // Pins current behaviour: the documented raw-payload fallback only triggers for a json null, malformed payloads throw
    @Test
    fun `GIVEN malformed evm json WHEN create THEN throws instead of raw fallback`() {
        // Act
        val error = runCatching {
            factory.create(NetworkType.ETHEREUM, accountAddress = "0xaa", unsignedTransaction = "not-json")
        }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(JsonEncodingException::class.java)
    }

    @Test
    fun `GIVEN evm tx without value WHEN create THEN value omitted`() {
        // Arrange
        val unsigned = """{"from":"0xaa","to":"0xbb","data":"0xcc","gasLimit":"0x1"}"""

        // Act
        val result = factory.create(NetworkType.BINANCE, accountAddress = "0xaa", unsignedTransaction = unsigned)

        // Assert
        assertThat((result.params as TransactionParams.Evm).params)
            .isEqualTo("""[{"from":"0xaa","to":"0xbb","data":"0xcc"}]""")
    }

    @Test
    fun `GIVEN solana address that is not base58 WHEN create THEN raw address kept`() {
        // Arrange
        // base58 alphabet has no '0', 'O', 'I', 'l', so "0OIl" does not decode; hex "0102" -> base64 "AQI="
        val accountAddress = "0OIl"

        // Act
        val result = factory.create(NetworkType.SOLANA, accountAddress = accountAddress, unsignedTransaction = "0102")

        // Assert
        assertThat(result.accountAddress).isEqualTo("0OIl")
        assertThat((result.params as TransactionParams.Solana).transactions).containsExactly("AQI=")
    }
}