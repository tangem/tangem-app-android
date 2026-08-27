package com.tangem.data.pay.store

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [DefaultTangemPayStorage.clearAll] and [DefaultTangemPayStorage.clearIsTangemPayDeactivated].
 *
 * Uses a real [AppPreferencesStore] over an in-memory [MockStateDataStore] so the assertions can look at
 * the raw preferences and tell *key removed* from *value overwritten* — the distinction the clearing path
 * depends on. Every case passes a `null` customer wallet address, which keeps the encrypted token storage
 * (an `AndroidSecureStorageV2` needing a real keystore) out of the picture.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayStorageTest {

    private val dataStore = MockStateDataStore(default = emptyPreferences())
    private val prefs = createAppPreferencesStore(
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = dataStore,
    )

    private val storage = DefaultTangemPayStorage(
        applicationContext = mockk<Context>(relaxed = true),
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
        sdkMoshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
        appPreferencesStore = prefs,
    )

    @BeforeEach
    fun resetStore() {
        runBlocking { dataStore.updateData { emptyPreferences() } }
    }

    @Test
    fun `GIVEN cached check result WHEN clearAll THEN it reads back as unknown and not as a cached false`() =
        runTest {
            // Arrange
            storage.storeCheckCustomerWalletResult(userWalletId = WALLET, isPaeraCustomer = true)

            // Act
            storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

            // Assert — null means "ask the server again"; false would keep the Tangem Pay block hidden
            assertThat(storage.checkCustomerWalletResult(WALLET)).isNull()
            assertThat(prefs.data.first()[PreferencesKeys.getTangemPayCheckCustomerByWalletId(WALLET)]).isNull()
        }

    @Test
    fun `GIVEN stored address WHEN clearAll THEN the address key is removed`() = runTest {
        // Arrange
        storage.storeCustomerWalletAddress(userWalletId = WALLET, customerWalletAddress = ADDRESS)

        // Act
        storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

        // Assert
        assertThat(storage.getCustomerWalletAddress(WALLET)).isNull()
        assertThat(prefs.data.first()[PreferencesKeys.getTangemPayCustomerWalletAddressKey(WALLET)]).isNull()
    }

    @Test
    fun `GIVEN hidden onboarding banner WHEN clearAll THEN the flag key is removed`() = runTest {
        // Arrange
        storage.storeHideOnboardingBanner(userWalletId = WALLET, hide = true)

        // Act
        storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

        // Assert
        assertThat(storage.getHideMainOnboardingBanner(WALLET)).isFalse()
        assertThat(prefs.data.first()[PreferencesKeys.getTangemPayHideOnboardingKey(WALLET)]).isNull()
    }

    @Test
    fun `GIVEN two wallets WHEN clearAll for one THEN the other one keeps its data`() = runTest {
        // Arrange
        storage.storeCheckCustomerWalletResult(userWalletId = WALLET, isPaeraCustomer = true)
        storage.storeCheckCustomerWalletResult(userWalletId = OTHER_WALLET, isPaeraCustomer = true)
        storage.storeCustomerWalletAddress(userWalletId = OTHER_WALLET, customerWalletAddress = ADDRESS)

        // Act
        storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

        // Assert
        assertThat(storage.checkCustomerWalletResult(OTHER_WALLET)).isTrue()
        assertThat(storage.getCustomerWalletAddress(OTHER_WALLET)).isEqualTo(ADDRESS)
    }

    @Test
    fun `GIVEN saved withdraw orders WHEN clearAll THEN the wallet's orders are dropped`() = runTest {
        // Arrange
        storage.storeWithdrawOrder(userWalletId = WALLET, data = WITHDRAW_ORDER)
        storage.storeActiveWithdrawOrderId(userWalletId = WALLET, orderId = ORDER_ID)

        // Act
        storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

        // Assert
        assertThat(storage.getWithdrawOrders(WALLET)).isEmpty()
        assertThat(storage.getActiveWithdrawOrderId(WALLET)).isNull()
    }

    @Test
    fun `GIVEN two wallets with withdraw orders WHEN clearAll for one THEN the other one keeps them`() = runTest {
        // Arrange
        storage.storeWithdrawOrder(userWalletId = WALLET, data = WITHDRAW_ORDER)
        storage.storeWithdrawOrder(userWalletId = OTHER_WALLET, data = WITHDRAW_ORDER)
        storage.storeActiveWithdrawOrderId(userWalletId = OTHER_WALLET, orderId = ORDER_ID)

        // Act
        storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

        // Assert
        assertThat(storage.getWithdrawOrders(OTHER_WALLET)).containsExactly(WITHDRAW_ORDER)
        assertThat(storage.getActiveWithdrawOrderId(OTHER_WALLET)).isEqualTo(ORDER_ID)
    }

    @Test
    fun `GIVEN deactivated marker WHEN clearIsTangemPayDeactivated THEN the key is removed`() = runTest {
        // Arrange
        storage.storeIsTangemPayDeactivated(WALLET)

        // Act
        storage.clearIsTangemPayDeactivated(WALLET)

        // Assert
        assertThat(storage.isTangemPayDeactivated(WALLET)).isFalse()
        assertThat(prefs.data.first()[PreferencesKeys.getTangemPayDeactivatedKey(WALLET)]).isNull()
    }

    @Test
    fun `GIVEN deactivated marker WHEN clearAll THEN it survives because disabling Pay must not reset it`() =
        runTest {
            // Arrange
            storage.storeIsTangemPayDeactivated(WALLET)

            // Act
            storage.clearAll(userWalletId = WALLET, customerWalletAddress = null)

            // Assert
            assertThat(storage.isTangemPayDeactivated(WALLET)).isTrue()
        }

    private companion object {
        val WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
        const val ADDRESS = "0xaddress"
        const val ORDER_ID = "order-1"
        val WITHDRAW_ORDER = TangemPayWithdrawState(
            orderId = ORDER_ID,
            exchangeData = TangemPayWithdrawExchangeState(
                txId = "tx-1",
                fromNetwork = "ethereum",
                fromAddress = "0xfrom",
                payInAddress = "0xpayin",
                payInExtraId = null,
            ),
            txHash = null,
        )
    }
}