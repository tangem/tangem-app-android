package com.tangem.features.details.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.impl.R
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ItemsBuilderTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val hotWalletRestrictionManager: HotWalletRestrictionManager = mockk()

    private val itemsBuilder = ItemsBuilder(
        router = router,
        hotWalletRestrictionManager = hotWalletRestrictionManager,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(router, hotWalletRestrictionManager)
        every { hotWalletRestrictionManager.isCreationEnabledSync() } returns false
    }

    @Test
    fun `GIVEN walletConnect available AND addressBook unavailable WHEN buildAll THEN standalone WalletConnect block`() {
        // Act
        val result = buildAll(isWalletConnectAvailable = true, isAddressBookAvailable = false)

        // Assert
        assertThat(result.map { it.id }).containsExactly(
            "wallet_connect",
            "user_wallet_list",
            "shop",
            "settings",
            "support",
        ).inOrder()
        assertThat(result.first()).isInstanceOf(DetailsItemUM.WalletConnect::class.java)
    }

    @Test
    fun `GIVEN walletConnect AND addressBook available WHEN buildAll THEN combined block with both items`() {
        // Act
        val result = buildAll(isWalletConnectAvailable = true, isAddressBookAvailable = true)

        // Assert
        assertThat(result.map { it.id }).containsExactly(
            "wallet_connect_address_book",
            "user_wallet_list",
            "shop",
            "settings",
            "support",
        ).inOrder()

        val block = result.first() as DetailsItemUM.WalletConnectAddressBookBlock
        assertThat(block.items.map { it::class.java }).containsExactly(
            DetailsItemUM.WalletConnectAddressBookBlock.Item.WalletConnect::class.java,
            DetailsItemUM.WalletConnectAddressBookBlock.Item.AddressBook::class.java,
        ).inOrder()
    }

    @Test
    fun `GIVEN walletConnect unavailable AND addressBook available WHEN buildAll THEN combined block with addressBook only`() {
        // Act
        val result = buildAll(isWalletConnectAvailable = false, isAddressBookAvailable = true)

        // Assert
        assertThat(result.map { it.id }).containsExactly(
            "wallet_connect_address_book",
            "user_wallet_list",
            "shop",
            "settings",
            "support",
        ).inOrder()

        val block = result.first() as DetailsItemUM.WalletConnectAddressBookBlock
        assertThat(block.items.map { it::class.java }).containsExactly(
            DetailsItemUM.WalletConnectAddressBookBlock.Item.AddressBook::class.java,
        )
    }

    @Test
    fun `GIVEN walletConnect AND addressBook unavailable WHEN buildAll THEN no walletConnect block`() {
        // Act
        val result = buildAll(isWalletConnectAvailable = false, isAddressBookAvailable = false)

        // Assert
        assertThat(result.map { it.id }).containsExactly(
            "user_wallet_list",
            "shop",
            "settings",
            "support",
        ).inOrder()
    }

    @Test
    fun `GIVEN creation enabled AND has mobile wallet WHEN buildAll THEN under section text is added`() {
        // Arrange
        every { hotWalletRestrictionManager.isCreationEnabledSync() } returns true

        // Act
        val result = buildAll(hasAnyMobileWallet = true)

        // Assert
        val underSectionText = result.filterIsInstance<DetailsItemUM.UnderSectionText>().single()
        assertThat(underSectionText).isEqualTo(
            DetailsItemUM.UnderSectionText(
                id = "only_one_mobile_wallet_explanation",
                text = resourceReference(R.string.only_one_mobile_wallet_explanation),
            ),
        )
    }

    @Test
    fun `GIVEN creation disabled OR no mobile wallet WHEN buildAll THEN no under section text`() {
        // Arrange — creation enabled but no mobile wallet
        every { hotWalletRestrictionManager.isCreationEnabledSync() } returns true

        // Act
        val noMobileWallet = buildAll(hasAnyMobileWallet = false)

        // Arrange — has mobile wallet but creation disabled
        every { hotWalletRestrictionManager.isCreationEnabledSync() } returns false
        val creationDisabled = buildAll(hasAnyMobileWallet = true)

        // Assert
        assertThat(noMobileWallet.filterIsInstance<DetailsItemUM.UnderSectionText>()).isEmpty()
        assertThat(creationDisabled.filterIsInstance<DetailsItemUM.UnderSectionText>()).isEmpty()
    }

    @Test
    fun `GIVEN any flags WHEN buildAll THEN unconditional blocks are always present`() {
        // Act — most restrictive combination: nothing optional is added
        val result = buildAll(
            isWalletConnectAvailable = false,
            isAddressBookAvailable = false,
            isSupportChatAvailable = false,
            hasAnyMobileWallet = false,
        )

        // Assert
        assertThat(result.map { it.id }).containsAtLeast(
            "user_wallet_list",
            "shop",
            "settings",
            "support",
        )
        assertThat(result.filterIsInstance<DetailsItemUM.UserWalletList>()).hasSize(1)

        val shop = result.single { it.id == "shop" } as DetailsItemUM.Basic
        assertThat(shop.items.map { it.id }).containsExactly("buy_tangem_wallet")

        val support = result.single { it.id == "support" } as DetailsItemUM.Basic
        assertThat(support.items.map { it.id }).containsExactly("support_email", "disclaimer").inOrder()
    }

    @Test
    fun `GIVEN shop block WHEN addTangemPayItem THEN tangem pay item appended to shop only`() {
        // Arrange
        val onClick: () -> Unit = {}
        val items: ImmutableList<DetailsItemUM> = persistentListOf(
            basicBlock(id = "shop", itemIds = listOf("buy_tangem_wallet")),
            basicBlock(id = "settings", itemIds = listOf("app_settings")),
        )

        // Act
        val result = itemsBuilder.addTangemPayItem(items = items, onClick = onClick)

        // Assert
        val shop = result.single { it.id == "shop" } as DetailsItemUM.Basic
        assertThat(shop.items.map { it.id }).containsExactly("buy_tangem_wallet", "get_tangem_pay").inOrder()

        val settings = result.single { it.id == "settings" } as DetailsItemUM.Basic
        assertThat(settings.items.map { it.id }).containsExactly("app_settings")
    }

    @Test
    fun `GIVEN shop block with tangem pay item WHEN removeTangemPayItem THEN item removed from shop only`() {
        // Arrange
        val items: ImmutableList<DetailsItemUM> = persistentListOf(
            basicBlock(id = "shop", itemIds = listOf("buy_tangem_wallet", "get_tangem_pay")),
            basicBlock(id = "settings", itemIds = listOf("app_settings")),
        )

        // Act
        val result = itemsBuilder.removeTangemPayItem(items = items)

        // Assert
        val shop = result.single { it.id == "shop" } as DetailsItemUM.Basic
        assertThat(shop.items.map { it.id }).containsExactly("buy_tangem_wallet")

        val settings = result.single { it.id == "settings" } as DetailsItemUM.Basic
        assertThat(settings.items.map { it.id }).containsExactly("app_settings")
    }

    private fun buildAll(
        isWalletConnectAvailable: Boolean = false,
        isAddressBookAvailable: Boolean = false,
        isSupportChatAvailable: Boolean = false,
        hasAnyMobileWallet: Boolean = false,
    ): ImmutableList<DetailsItemUM> = itemsBuilder.buildAll(
        isWalletConnectAvailable = isWalletConnectAvailable,
        isAddressBookAvailable = isAddressBookAvailable,
        isSupportChatAvailable = isSupportChatAvailable,
        hasAnyMobileWallet = hasAnyMobileWallet,
        userWalletId = USER_WALLET_ID,
        onSupportEmailClick = {},
        onSupportChatClick = {},
        onBuyClick = {},
    )

    private fun basicBlock(id: String, itemIds: List<String>): DetailsItemUM.Basic = DetailsItemUM.Basic(
        id = id,
        items = itemIds.map { itemId ->
            DetailsItemUM.Basic.Item(
                id = itemId,
                block = BlockUM(
                    text = resourceReference(R.string.common_unknown_error),
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                ),
            )
        }.toImmutableList(),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}