package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.domain.models.staking.StakingID
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultStakingYieldsStoreTest {

    private val dataStore: DataStore<List<YieldDTO>> = mockk()
    private val hangingDataStore: DataStore<List<YieldDTO>> = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(dataStore, hangingDataStore)
    }

    private fun yieldDto(id: String) = MockYieldDTOFactory.create(StakingID(integrationId = id, address = "0x1"))

    private val ton = yieldDto(MockStaking.TON_ID)
    private val tonChanged = yieldDto(MockStaking.TON_ID).copy(apy = BigDecimal("0.1"))
    private val sol = yieldDto(MockStaking.SOLANA_ID)
    private val cosmos = yieldDto(MockStaking.COSMOS_ID)

    private fun createStore(vararg initial: YieldDTO) =
        DefaultStakingYieldsStore(MockStateDataStore<List<YieldDTO>>(default = initial.toList()))

    private fun createStoreWithoutData(): DefaultStakingYieldsStore {
        every { dataStore.data } returns emptyFlow()
        return DefaultStakingYieldsStore(dataStore)
    }

    @Nested
    inner class Store {

        @Test
        fun `GIVEN stored yields WHEN store with force THEN list replaced`() = runTest {
            // Arrange
            val store = createStore(ton, sol)

            // Act
            store.store(listOf(cosmos), force = true)

            // Assert
            assertThat(store.getSync()).isEqualTo(listOf(cosmos))
        }

        @Test
        fun `GIVEN stored yields WHEN store without force THEN same id replaced in place and new id appended`() =
            runTest {
                // Arrange
                val store = createStore(ton, sol)

                // Act
                store.store(listOf(tonChanged, cosmos), force = false)

                // Assert
                assertThat(store.getSync()).containsExactly(tonChanged, sol, cosmos).inOrder()
            }

        // Pins current behaviour: merge looks up indexes in the pre-update list, so duplicates within a batch are not collapsed
        @Test
        fun `GIVEN duplicate ids in one batch WHEN store without force THEN both appended`() = runTest {
            // Arrange
            val store = createStore()

            // Act
            store.store(listOf(ton, tonChanged), force = false)

            // Assert
            assertThat(store.getSync()).containsExactly(ton, tonChanged).inOrder()
        }

        @Test
        fun `GIVEN existing yield without id WHEN store new yield without id THEN replaced`() = runTest {
            // Arrange
            val store = createStore(yieldDto(MockStaking.TON_ID).copy(id = null))
            val replacement = yieldDto(MockStaking.SOLANA_ID).copy(id = null)

            // Act
            store.store(listOf(replacement), force = false)

            // Assert
            assertThat(store.getSync()).isEqualTo(listOf(replacement))
        }
    }

    @Nested
    inner class Read {

        @Test
        fun `GIVEN data store emits nothing WHEN getSync THEN empty list`() = runTest {
            // Arrange
            val store = createStoreWithoutData()

            // Act
            val actual = store.getSync()

            // Assert
            assertThat(actual).isEqualTo(emptyList<YieldDTO>())
        }

        @Test
        fun `GIVEN stored yields WHEN getSyncWithTimeout THEN returns them immediately`() = runTest {
            // Arrange
            val store = createStore(ton)

            // Act
            val actual = store.getSyncWithTimeout()

            // Assert
            assertThat(actual).isEqualTo(listOf(ton))
            assertThat(testScheduler.currentTime).isEqualTo(0L)
        }

        @Test
        fun `GIVEN data store never emits WHEN getSyncWithTimeout THEN null after 15 seconds`() = runTest {
            // Arrange
            every { hangingDataStore.data } returns flow { awaitCancellation() }
            val store = DefaultStakingYieldsStore(hangingDataStore)

            // Act
            val actual = store.getSyncWithTimeout()

            // Assert
            assertThat(actual).isNull()
            assertThat(testScheduler.currentTime).isEqualTo(15_000L)
        }

        @Test
        fun `GIVEN data store completes empty WHEN getSyncWithTimeout THEN null without waiting`() = runTest {
            // Arrange
            val store = createStoreWithoutData()

            // Act
            val actual = store.getSyncWithTimeout()

            // Assert
            assertThat(actual).isNull()
            assertThat(testScheduler.currentTime).isEqualTo(0L)
        }
    }
}