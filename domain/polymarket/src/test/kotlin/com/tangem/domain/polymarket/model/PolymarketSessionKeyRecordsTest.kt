package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionKeyRecordsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN records of an address WHEN newestFor THEN the newest record wins`(model: NewestForModel) {
        // Act
        val actual = model.records.newestFor(model.address)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN records of several addresses WHEN newestPerAddress THEN each address keeps its newest record`() {
        // Arrange
        val records = listOf(OLD_RECORD, OTHER_RECORD, NEW_RECORD.copy(address = CHECKSUMMED_ADDRESS))

        // Act
        val actual = records.newestPerAddress()

        // Assert
        assertThat(actual).containsExactly(NEW_RECORD.copy(address = CHECKSUMMED_ADDRESS), OTHER_RECORD)
    }

    @Test
    fun `GIVEN no records WHEN newestPerAddress THEN nothing is returned`() {
        // Act
        val actual = emptyList<PolymarketSessionKey>().newestPerAddress()

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun provideTestModels() = listOf(
        NewestForModel(
            case = "oldest record listed first",
            records = listOf(OLD_RECORD, NEW_RECORD),
            expected = NEW_RECORD,
        ),
        NewestForModel(
            case = "newest record listed first",
            records = listOf(NEW_RECORD, OLD_RECORD),
            expected = NEW_RECORD,
        ),
        NewestForModel(
            case = "address in another case",
            records = listOf(OLD_RECORD, NEW_RECORD),
            address = CHECKSUMMED_ADDRESS,
            expected = NEW_RECORD,
        ),
        NewestForModel(
            case = "record stored in another case",
            records = listOf(OLD_RECORD, NEW_RECORD.copy(address = CHECKSUMMED_ADDRESS)),
            expected = NEW_RECORD.copy(address = CHECKSUMMED_ADDRESS),
        ),
        NewestForModel(
            case = "a newer record of another address is ignored",
            records = listOf(OLD_RECORD, OTHER_RECORD),
            expected = OLD_RECORD,
        ),
        NewestForModel(case = "no record of the address", records = listOf(OTHER_RECORD), expected = null),
        NewestForModel(case = "no records", records = emptyList(), expected = null),
    )

    data class NewestForModel(
        val case: String,
        val records: List<PolymarketSessionKey>,
        val address: String = ADDRESS,
        val expected: PolymarketSessionKey?,
    ) {
        override fun toString(): String = case
    }

    private companion object {
        const val ADDRESS = "0x5e55000000000000000000000000000000000abc"
        const val CHECKSUMMED_ADDRESS = "0x5E55000000000000000000000000000000000ABC"
        const val OTHER_ADDRESS = "0x5e55000000000000000000000000000000000def"
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)

        val OLD_RECORD = record(address = ADDRESS, status = PolymarketSessionKeyStatus.REVOKED, createdAt = NOW)
        val NEW_RECORD = record(address = ADDRESS, createdAt = NOW.plusSeconds(60))
        val OTHER_RECORD = record(address = OTHER_ADDRESS, createdAt = NOW.plusSeconds(120))

        fun record(
            address: String,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            createdAt: Instant,
        ) = PolymarketSessionKey(
            address = address,
            deviceId = "device",
            deviceName = "Pixel",
            isEnabled = true,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = NOW.plusSeconds(1_000_000),
            renewAfter = null,
            authorizedAt = null,
            createdAt = createdAt,
        )
    }
}