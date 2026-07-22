package com.tangem.data.addressbook.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAddressBookBlobStoreTest {

    private lateinit var store: DefaultAddressBookBlobStore

    @BeforeEach
    fun setup() {
        store = DefaultAddressBookBlobStore(
            dataStore = MockStateDataStore(default = emptyMap()),
        )
    }

    @Test
    fun `GIVEN blob WHEN storeBlob THEN getBlob emits it`() = runTest {
        // Arrange
        val blob = createBlob(walletId = WALLET_A)

        // Act
        store.storeBlob(blob)

        // Assert
        assertThat(store.getBlob(UserWalletId(WALLET_A)).first()).isEqualTo(blob)
        assertThat(store.getBlobSync(UserWalletId(WALLET_A))).isEqualTo(blob)
    }

    @Test
    fun `GIVEN blobs for two wallets WHEN getBlob walletA THEN only walletA blob emitted`() = runTest {
        // Arrange
        val blobA = createBlob(walletId = WALLET_A)
        val blobB = createBlob(walletId = WALLET_B)
        store.storeBlob(blobA)
        store.storeBlob(blobB)

        // Act
        val result = store.getBlob(UserWalletId(WALLET_A)).first()

        // Assert
        assertThat(result).isEqualTo(blobA)
    }

    @Test
    fun `GIVEN blobs for two wallets WHEN getBlobs THEN only requested wallets returned`() = runTest {
        // Arrange
        val blobA = createBlob(walletId = WALLET_A)
        val blobB = createBlob(walletId = WALLET_B)
        store.storeBlob(blobA)
        store.storeBlob(blobB)

        // Act
        val result = store.getBlobs(setOf(UserWalletId(WALLET_A), UserWalletId(WALLET_B))).first()
        val onlyA = store.getBlobs(setOf(UserWalletId(WALLET_A))).first()

        // Assert
        assertThat(result).containsExactly(blobA, blobB)
        assertThat(onlyA).containsExactly(blobA)
    }

    @Test
    fun `GIVEN stored blob WHEN deleteBlob THEN getBlob emits null`() = runTest {
        // Arrange
        val blob = createBlob(walletId = WALLET_A)
        store.storeBlob(blob)

        // Act
        store.deleteBlob(UserWalletId(WALLET_A))

        // Assert
        assertThat(store.getBlob(UserWalletId(WALLET_A)).first()).isNull()
        assertThat(store.getBlobSync(UserWalletId(WALLET_A))).isNull()
    }

    private fun createBlob(walletId: String): AddressBookBlob = AddressBookBlob(
        walletId = walletId,
        updatedAt = "2026-05-22T09:00:00.000Z",
        nonce = "00112233445566778899aabb",
        ciphertext = "deadbeef",
        authTag = "cafebabecafebabecafebabecafebabe",
    )

    private companion object {
        const val WALLET_A = "0a0a0a"
        const val WALLET_B = "0b0b0b"
    }
}