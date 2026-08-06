package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultExpressAssetsStoreTest {

    private val persistenceStore = mockk<DataStore<AssetsByWalletId>>(relaxed = true) {
        every { data } returns flowOf(emptyMap())
        coEvery { updateData(any()) } returns emptyMap()
    }
    private val runtimeStore = RuntimeSharedMapStore<UserWalletId, List<Asset>>()

    private val store = DefaultExpressAssetsStore(persistenceStore = persistenceStore, runtimeStore = runtimeStore)

    private val walletId = mockk<UserWalletId>()

    @Test
    fun `GIVEN nothing stored WHEN getSyncOrNull THEN returns null`() = runTest {
        every { walletId.stringValue } returns "wallet-1"

        assertThat(store.getSyncOrNull(walletId)).isNull()
    }

    @Test
    fun `GIVEN assets stored WHEN getSyncOrNull THEN returns them from runtime`() = runTest {
        // Arrange
        every { walletId.stringValue } returns "wallet-1"
        val assets = listOf(mockk<Asset>())
        store.store(walletId, assets)

        // Act
        val actual = store.getSyncOrNull(walletId)

        // Assert
        assertThat(actual).isEqualTo(assets)
    }
}