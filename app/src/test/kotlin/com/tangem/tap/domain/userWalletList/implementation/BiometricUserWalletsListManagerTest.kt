package com.tangem.tap.domain.userWalletList.implementation

import com.google.common.truth.Truth
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.card.ScanResponseMockFactory
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class BiometricUserWalletsListManagerTest(private val model: Model) {

    private val manager = BiometricUserWalletsListManager(
        keysRepository = mockk(),
        publicInformationRepository = mockk(),
        sensitiveInformationRepository = mockk(),
        selectedUserWalletRepository = mockk(),
    )

    @Test
    fun testFindAvailableUserWallet() {
        with(model) {
            val actual = userWallets.findAvailableUserWallet(prevSelectedIndex)

            Truth.assertThat(actual).isEqualTo(newSelectedWallet)
        }
    }

    private fun List<UserWallet>.findAvailableUserWallet(prevSelectedIndex: Int): UserWallet? {
        return manager::class
            .declaredFunctions
            .firstOrNull { it.name == "findAvailableUserWallet" }
            ?.apply { isAccessible = true }
            ?.call(manager, this, prevSelectedIndex)
            as? UserWallet
    }

    data class Model(
        val userWallets: List<UserWallet>,
        val prevSelectedIndex: Int,
        val newSelectedWallet: UserWallet?,
    )

    private companion object {

        val userWallet0 = createUserWallet(id = "0", isLocked = false)
        val lockedUserWallet0 = createUserWallet(id = "0", isLocked = true)

        val userWallet1 = createUserWallet(id = "1", isLocked = false)
        val lockedUserWallet1 = createUserWallet(id = "1", isLocked = true)

        val userWallet2 = createUserWallet(id = "2", isLocked = false)
        val lockedUserWallet2 = createUserWallet(id = "2", isLocked = true)

        val userWallet3 = createUserWallet(id = "3", isLocked = false)
        val lockedUserWallet3 = createUserWallet(id = "3", isLocked = true)

        val unlockedWallets = listOf(userWallet0, userWallet1, userWallet2, userWallet3)
        val lockedWallets = listOf(lockedUserWallet0, lockedUserWallet1, lockedUserWallet2, lockedUserWallet3)

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> {
            return listOf(
                Model(userWallets = emptyList(), prevSelectedIndex = 0, newSelectedWallet = null),
                *getTestsWithUnlockedWallets().toTypedArray(),
                *getTestsIfPrevSelectedIndexIs0().toTypedArray(),
                *getTestsIfPrevSelectedIndexIsLastIndex().toTypedArray(),
                *getTestsIfNewSelectedIndexIsNearby().toTypedArray(),
                *getTestsIfNewSelectedIndexIsThroughOne().toTypedArray(),
            )
        }

        fun getTestsWithUnlockedWallets() = listOf(
            // [*0*, 1, 2, 3, 4] => delete 0 => [1, 2, 3, 4] => select 1 => [*1*, 2, 3, 4]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 0, newSelectedWallet = userWallet0),
            // [0, *1*, 2, 3, 4] => delete 1 => [0, 2, 3, 4] => select 2 => [0, *2*, 3, 4]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 1, newSelectedWallet = userWallet1),
            // [0, 1, *2*, 3, 4] => delete 2 => [0, 1, 3, 4] => select 3 => [0, 1, *3*, 4]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 2, newSelectedWallet = userWallet2),
            // [0, 1, 2, *3*, 4] => delete 3 => [0, 1, 2, 4] => select 4 => [0, 1, 2, 4]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 3, newSelectedWallet = userWallet3),
            // [0, 1, 2, 3, *4*] => delete 4 => [0, 1, 2, 3] => select 3 => [0, 1, 2, *3*]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 4, newSelectedWallet = userWallet3),
        )

        fun getTestsIfPrevSelectedIndexIs0() = listOf(
            // [*0*, -1-, 2, 3, 4] => delete 0 => [-1-, 2, 3, 4] => select 2 => [-1-, *2*, 3, 4]
            Model(
                userWallets = listOf(lockedUserWallet0, userWallet1, userWallet2, userWallet3),
                prevSelectedIndex = 0,
                newSelectedWallet = userWallet1,
            ),
            // [*0*, -1-, -2-, 3, 4] => delete 0 => [-1-, -2-, 3, 4] => select 3 => [-1-, -2-, *3*, 4]
            Model(
                userWallets = listOf(lockedUserWallet0, lockedUserWallet1, userWallet2, userWallet3),
                prevSelectedIndex = 0,
                newSelectedWallet = userWallet2,
            ),
            // [*0*, -1-, -2-, -3-, 4] => delete 0 => [-1-, -2-, -3-, 4] => select 4 => [-1-, -2-, -3-, *4*]
            Model(
                userWallets = listOf(lockedUserWallet0, lockedUserWallet1, lockedUserWallet2, userWallet3),
                prevSelectedIndex = 0,
                newSelectedWallet = userWallet3,
            ),
            // [*0*, -1-, -2-, -3-, -4-] => delete 0 => [-1-, -2-, -3-, -4-] => select 1 => [*-1-*, -2-, -3-, -4-]
            Model(userWallets = lockedWallets, prevSelectedIndex = 0, newSelectedWallet = lockedUserWallet0),
        )

        fun getTestsIfPrevSelectedIndexIsLastIndex() = listOf(
            // [0, 1, 2, -3-, *4*] => delete 4 => [0, 1, 2, -3-] => select 2 => [0, 1, *2*, -3-]
            Model(
                userWallets = listOf(userWallet0, userWallet1, userWallet2, lockedUserWallet3),
                prevSelectedIndex = 4,
                newSelectedWallet = userWallet2,
            ),
            // [0, 1, -2-, -3-, *4*] => delete 4 => [0, 1, -2-, -3-] => select 1 => [0, *1*, -2-, -3-]
            Model(
                userWallets = listOf(userWallet0, userWallet1, lockedUserWallet2, lockedUserWallet3),
                prevSelectedIndex = 4,
                newSelectedWallet = userWallet1,
            ),
            // [0, -1-, -2-, -3-, *4*] => delete 4 => [0, -1-, -2-, -3-] => select 0 => [*0*, -1-, -2-, -3-]
            Model(
                userWallets = listOf(userWallet0, lockedUserWallet1, lockedUserWallet2, lockedUserWallet3),
                prevSelectedIndex = 4,
                newSelectedWallet = userWallet0,
            ),
            // [-0-, -1-, -2-, -3-, *4*] => delete 4 => [-0-, -1-, -2-, -3-] => select 3 => [-0-, -1-, -2-, *-3-*]
            Model(userWallets = lockedWallets, prevSelectedIndex = 4, newSelectedWallet = lockedUserWallet3),
        )

        fun getTestsIfNewSelectedIndexIsNearby() = listOf(
            // [0, *1*, 2, 3, 4] => delete 1 => [0, 2, 3, 4] => select 2 => [0, *2*, 3, 4]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 1, newSelectedWallet = userWallet1),
            // [0, *1*, -2-, 3, 4] => delete 1 => [0, -2-, 3, 4] => select 3 => [0, -2-, *3*, 4]
            Model(
                userWallets = listOf(userWallet0, lockedUserWallet1, userWallet2, userWallet3),
                prevSelectedIndex = 1,
                newSelectedWallet = userWallet2,
            ),
            // [0, *1*, -2-, -3-, 4] => delete 1 => [0, -2-, -3-, 4] => select 0 => [*0*, -2-, -3-, 4]
            Model(
                userWallets = listOf(userWallet0, lockedUserWallet1, lockedUserWallet2, userWallet3),
                prevSelectedIndex = 1,
                newSelectedWallet = userWallet0,
            ),
            // [0, 1, 2, *3*, 4] => delete 3 => [0, 1, 2, 4] => select 4 => [0, 1, 2, *4*]
            Model(userWallets = unlockedWallets, prevSelectedIndex = 3, newSelectedWallet = userWallet3),
            // [0, 1, 2, *3*, -4-] => delete 3 => [0, 1, 2, -4-] => select 2 => [0, 1, *2*, -4-]
            Model(
                userWallets = listOf(userWallet0, userWallet1, userWallet2, lockedUserWallet3),
                prevSelectedIndex = 3,
                newSelectedWallet = userWallet2,
            ),
        )

        fun getTestsIfNewSelectedIndexIsThroughOne() = listOf(
            // [-0-, *1*, -2-, 3, 4] => delete 1 => [-0-, -2-, 3, 4] => select 3 => [-0-, -2-, *3*, 4]
            Model(
                userWallets = listOf(lockedUserWallet0, lockedUserWallet1, userWallet2, userWallet3),
                prevSelectedIndex = 1,
                newSelectedWallet = userWallet2,
            ),
            // [-0-, *1*, -2-, -3-, 4] => delete 1 => [-0-, -2-, -3-, 4] => select 4 => [-0-, -2-, -3-, *4*]
            Model(
                userWallets = listOf(lockedUserWallet0, lockedUserWallet1, lockedUserWallet2, userWallet3),
                prevSelectedIndex = 1,
                newSelectedWallet = userWallet3,
            ),
            // [0, 1, -2-, *3*, -4-] => delete 3 => [0, 1, -2-, -4-] => select 1 => [0, *1*, -2-, -4-]
            Model(
                userWallets = listOf(userWallet0, userWallet1, lockedUserWallet2, lockedUserWallet3),
                prevSelectedIndex = 3,
                newSelectedWallet = userWallet1,
            ),
            // [0, -1-, -2-, *3*, -4-] => delete 3 => [*0*, -1-, -2-, -4-] => select 0 => [0, -1-, -2-, -4-]
            Model(
                userWallets = listOf(userWallet0, lockedUserWallet1, lockedUserWallet2, lockedUserWallet3),
                prevSelectedIndex = 3,
                newSelectedWallet = userWallet0,
            ),
        )

        fun createUserWallet(id: String, isLocked: Boolean): UserWallet {
            return UserWallet(
                name = "Wallet $id",
                walletId = UserWalletId(stringValue = id),
                artworkUrl = "",
                cardsInWallet = emptySet(),
                isMultiCurrency = true,
                hasBackupError = false,
                scanResponse = ScanResponseMockFactory.create(
                    cardConfig = GenericCardConfig(maxWalletCount = 1),
                    derivedKeys = emptyMap(),
                ).let {
                    if (isLocked) {
                        it.copy(
                            card = it.card.copy(wallets = emptyList()),
                        )
                    } else {
                        it
                    }
                },
            )
        }
    }
}