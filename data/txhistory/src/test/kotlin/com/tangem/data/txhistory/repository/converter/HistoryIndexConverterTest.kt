package com.tangem.data.txhistory.repository.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import io.mockk.every
import io.mockk.mockk
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Verifies the indexing rules that build [HistoryIndexEntity] rows: a swap is indexed under both its from- and

 * into the sort time.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HistoryIndexConverterTest {

    @Test
    fun `GIVEN swap with distinct from and payout WHEN indexed THEN one row per address`() {
        // Arrange
        val entity = exchange(txId = "tx1", from = "addrA", payout = "addrB", createdAt = CREATED_AT)

        // Act
        val rows = entity.toHistoryIndexEntities()

        // Assert
        assertThat(rows).containsExactly(
            indexRow(HistoryIndexEntity.Type.EXCHANGE, "tx1", "addrA"),
            indexRow(HistoryIndexEntity.Type.EXCHANGE, "tx1", "addrB"),
        )
    }

    @Test
    fun `GIVEN swap with equal from and payout WHEN indexed THEN de-duplicated to one row`() {
        // Arrange
        val entity = exchange(txId = "tx1", from = "same", payout = "same", createdAt = CREATED_AT)

        // Act
        val rows = entity.toHistoryIndexEntities()

        // Assert
        assertThat(rows).containsExactly(indexRow(HistoryIndexEntity.Type.EXCHANGE, "tx1", "same"))
    }

    @Test
    fun `GIVEN onramp WHEN indexed THEN single row under payout address`() {
        // Arrange
        val entity = onramp(txId = "tx2", payout = "addrP", createdAt = CREATED_AT)

        // Act
        val row = entity.toHistoryIndexEntity()

        // Assert
        assertThat(row).isEqualTo(indexRow(HistoryIndexEntity.Type.ONRAMP, "tx2", "addrP"))
    }

    @Test
    fun `GIVEN ISO-8601 created_at WHEN indexed THEN parsed into sort time millis`() {
        // Arrange
        val entity = onramp(txId = "tx2", payout = "addrP", createdAt = "2026-07-03T10:00:00.000Z")

        // Act
        val row = entity.toHistoryIndexEntity()

        // Assert
        assertThat(row.sortTimeMillis).isEqualTo(DateTime.parse("2026-07-03T10:00:00.000Z").millis)
    }

    private fun exchange(txId: String, from: String, payout: String, createdAt: String) =
        mockk<ExpressExchangeEntity> {
            every { this@mockk.txId } returns txId
            every { fromAddress } returns from
            every { payoutAddress } returns payout
            every { this@mockk.createdAt } returns createdAt
        }

    private fun onramp(txId: String, payout: String, createdAt: String) =
        mockk<ExpressOnrampEntity> {
            every { this@mockk.txId } returns txId
            every { payoutAddress } returns payout
            every { this@mockk.createdAt } returns createdAt
        }

    private fun indexRow(type: HistoryIndexEntity.Type, txId: String, address: String) = HistoryIndexEntity(
        type = type.value,
        entityId = txId,
        address = address,
        sortTimeMillis = DateTime.parse(CREATED_AT).millis,
    )

    private companion object {
        const val CREATED_AT = "2026-07-03T10:00:00.000Z"
    }
}