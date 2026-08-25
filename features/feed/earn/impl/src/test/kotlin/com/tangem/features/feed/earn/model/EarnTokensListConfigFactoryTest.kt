package com.tangem.features.feed.earn.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.features.feed.earn.createEarnFilter
import com.tangem.features.feed.earn.createEarnHttpError
import com.tangem.features.feed.earn.createEarnNetwork
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The config is the backend query the Best opportunities list is reloaded with, so every filter
 * combination is asserted as a whole [EarnTokensListConfig].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTokensListConfigFactoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun createEarnTokensListConfig(model: ConfigModel) {
        // Act
        val actual = createEarnTokensListConfig(
            filter = model.filter,
            earnNetworks = model.earnNetworks,
            isForEarn = model.isForEarn,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ConfigModel(
            name = "GIVEN no filter WHEN config created THEN neither type nor networks narrow the query",
            filter = null,
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = null, networks = null, isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN all types WHEN config created THEN type is null",
            filter = createEarnFilter(type = EarnFilterType.ALL),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = null, networks = null, isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN staking type WHEN config created THEN type is staking",
            filter = createEarnFilter(type = EarnFilterType.STAKING),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = "staking", networks = null, isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN yield type WHEN config created THEN type is yield",
            filter = createEarnFilter(type = EarnFilterType.YIELD),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = "yield", networks = null, isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN all networks WHEN config created THEN networks are null",
            filter = createEarnFilter(network = EarnFilterNetwork.AllNetworks(isSelected = true)),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = null, networks = null, isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN my networks WHEN networks are not loaded THEN the query asks for no network at all",
            filter = createEarnFilter(network = EarnFilterNetwork.MyNetworks(isSelected = true)),
            earnNetworks = null,
            expected = EarnTokensListConfig(type = null, networks = listOf(NO_NETWORK), isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN my networks WHEN the network list failed THEN networks are null",
            filter = createEarnFilter(network = EarnFilterNetwork.MyNetworks(isSelected = true)),
            earnNetworks = createEarnHttpError().left(),
            expected = EarnTokensListConfig(type = null, networks = null, isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN my networks WHEN some networks are added THEN only the added ids are queried",
            filter = createEarnFilter(network = EarnFilterNetwork.MyNetworks(isSelected = true)),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = null, networks = listOf("ethereum"), isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN my networks WHEN no network is added THEN the query asks for no network at all",
            filter = createEarnFilter(network = EarnFilterNetwork.MyNetworks(isSelected = true)),
            earnNetworks = listOf(
                createEarnNetwork(networkId = "ethereum", isAdded = false),
                createEarnNetwork(networkId = "solana", isAdded = false),
            ).right(),
            expected = EarnTokensListConfig(type = null, networks = listOf(NO_NETWORK), isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN my networks WHEN the network list is empty THEN the query asks for no network at all",
            filter = createEarnFilter(network = EarnFilterNetwork.MyNetworks(isSelected = true)),
            earnNetworks = emptyList<com.tangem.domain.models.earn.EarnNetwork>().right(),
            expected = EarnTokensListConfig(type = null, networks = listOf(NO_NETWORK), isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN a specific network WHEN config created THEN only that id is queried",
            filter = createEarnFilter(
                network = EarnFilterNetwork.Specific(
                    isSelected = true,
                    id = "solana",
                    symbol = "SOL",
                    fullName = "Solana",
                ),
            ),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = null, networks = listOf("solana"), isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN staking on a specific network WHEN config created THEN both narrow the query",
            filter = createEarnFilter(
                network = EarnFilterNetwork.Specific(
                    isSelected = true,
                    id = "solana",
                    symbol = "SOL",
                    fullName = "Solana",
                ),
                type = EarnFilterType.STAKING,
            ),
            earnNetworks = loadedNetworks,
            expected = EarnTokensListConfig(type = "staking", networks = listOf("solana"), isForEarn = false),
        ),
        ConfigModel(
            name = "GIVEN isForEarn WHEN config created THEN the flag reaches the config",
            filter = createEarnFilter(),
            earnNetworks = loadedNetworks,
            isForEarn = true,
            expected = EarnTokensListConfig(type = null, networks = null, isForEarn = true),
        ),
    )

    internal data class ConfigModel(
        val name: String,
        val filter: EarnFilter?,
        val earnNetworks: EarnNetworks?,
        val isForEarn: Boolean = false,
        val expected: EarnTokensListConfig,
    ) {
        override fun toString(): String = name
    }

    private companion object {

        /** Backend contract: this id makes the response come back empty. */
        const val NO_NETWORK = "-1"

        val loadedNetworks: EarnNetworks = listOf(
            createEarnNetwork(networkId = "ethereum", fullName = "Ethereum", isAdded = true),
            createEarnNetwork(networkId = "solana", fullName = "Solana", isAdded = false),
        ).right()
    }
}