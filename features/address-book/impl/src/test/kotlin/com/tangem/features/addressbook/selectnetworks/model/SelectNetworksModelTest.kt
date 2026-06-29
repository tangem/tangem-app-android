package com.tangem.features.addressbook.selectnetworks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.addressbook.common.SupportedNetworksMatcher
import com.tangem.features.addressbook.selectnetworks.DefaultSelectNetworksComponent
import com.tangem.features.addressbook.selectnetworks.state.SelectNetworksStateController
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SelectNetworksModelTest {

    private val supportedNetworksMatcher: SupportedNetworksMatcher = mockk()

    private val ethereum = Blockchain.Ethereum
    private val bsc = Blockchain.BSC

    private var model: SelectNetworksModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(supportedNetworksMatcher)
        // The address resolves to two networks unless a test overrides it.
        every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(ethereum, bsc)
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN no prior selection WHEN created THEN nothing selected AND done disabled`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert — all matched networks are listed but none is checked by default.
        val state = model.state.value
        assertThat(state.networks.map { it.name }).containsExactly(ethereum.fullName, bsc.fullName)
        assertThat(state.networks.none { it.isSelected }).isTrue()
        assertThat(state.doneButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN explicit selection WHEN created THEN only those networks selected`() = runTest {
        // Act
        val model = createModel(testScope = this, selectedNetworkIds = listOf(ethereum.toNetworkId()))
        advanceUntilIdle()

        // Assert
        val networks = model.state.value.networks
        assertThat(networks.first { it.id == ethereum.toNetworkId() }.isSelected).isTrue()
        assertThat(networks.first { it.id == bsc.toNetworkId() }.isSelected).isFalse()
    }

    @Test
    fun `GIVEN nothing selected WHEN a network toggled on THEN it becomes selected AND done enabled`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.networks.first { it.id == ethereum.toNetworkId() }.onCheckedChange()
        advanceUntilIdle()

        // Assert
        val networks = model.state.value.networks
        assertThat(networks.first { it.id == ethereum.toNetworkId() }.isSelected).isTrue()
        assertThat(networks.first { it.id == bsc.toNetworkId() }.isSelected).isFalse()
        assertThat(model.state.value.doneButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN a selected network toggled off THEN done disabled again`() = runTest {
        // Arrange
        val model = createModel(testScope = this, selectedNetworkIds = listOf(ethereum.toNetworkId()))
        advanceUntilIdle()

        // Act
        model.state.value.networks.first { it.id == ethereum.toNetworkId() }.onCheckedChange()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.networks.none { it.isSelected }).isTrue()
        assertThat(model.state.value.doneButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN query WHEN typed THEN list filtered by network name`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.searchBar.onQueryChange(ethereum.fullName)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.networks.map { it.name }).containsExactly(ethereum.fullName)
    }

    @Test
    fun `GIVEN selected networks WHEN done clicked THEN onDone called with them`() = runTest {
        // Arrange
        var result: Set<String>? = null
        val model = createModel(testScope = this, onDone = { result = it })
        advanceUntilIdle()
        model.state.value.networks.first { it.id == ethereum.toNetworkId() }.onCheckedChange()
        advanceUntilIdle()

        // Act
        model.state.value.doneButton.onClick()

        // Assert
        assertThat(result).containsExactly(ethereum.toNetworkId())
    }

    @Test
    fun `GIVEN no networks selected WHEN done clicked THEN onDone not called`() = runTest {
        // Arrange
        var result: Set<String>? = null
        val model = createModel(testScope = this, onDone = { result = it })
        advanceUntilIdle()

        // Act — nothing selected by default.
        model.state.value.doneButton.onClick()

        // Assert
        assertThat(result).isNull()
    }

    private fun createModel(
        testScope: TestScope,
        selectedNetworkIds: List<String> = emptyList(),
        onDone: (Set<String>) -> Unit = {},
        params: DefaultSelectNetworksComponent.Params = DefaultSelectNetworksComponent.Params(
            address = ADDRESS,
            selectedNetworkIds = selectedNetworkIds,
            onBackClick = {},
            onDone = onDone,
        ),
        paramsContainer: ParamsContainer = MutableParamsContainer(value = params),
    ): SelectNetworksModel {
        return SelectNetworksModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            supportedNetworksMatcher = supportedNetworksMatcher,
            stateController = SelectNetworksStateController(),
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        const val ADDRESS = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
    }
}