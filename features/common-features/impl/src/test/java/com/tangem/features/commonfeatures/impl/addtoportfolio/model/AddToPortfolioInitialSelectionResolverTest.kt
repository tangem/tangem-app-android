package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddAccount
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddWallet
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddToPortfolioInitialSelectionResolverTest {

    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency = mockk()
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase = mockk()

    private val tokenParams: RawMarketToken = mockk()

    private lateinit var resolver: AddToPortfolioInitialSelectionResolver

    @BeforeEach
    fun setup() {
        clearMocks(getTokenMarketCryptoCurrency, networkHasDerivationUseCase)
        coEvery { getTokenMarketCryptoCurrency(any(), any(), any(), any()) } returns null
        every { networkHasDerivationUseCase(any(), any()) } returns false.right()

        resolver = AddToPortfolioInitialSelectionResolver(
            getTokenMarketCryptoCurrency = getTokenMarketCryptoCurrency,
            networkHasDerivationUseCase = networkHasDerivationUseCase,
        )
    }

    @Test
    fun `GIVEN no wallets in data WHEN resolve THEN return null`() = runTest {
        val data = availableData(wallets = emptyMap())

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = null,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result).isNull()
    }

    @Test
    fun `GIVEN accountToAdd is provided WHEN resolve THEN use it instead of looking up account`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val mainAccount = availableAccount()
        val explicitAccount = availableAccount(availableToAddNetworks = setOf(BITCOIN))

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to mainAccount),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(BITCOIN),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
            accountToAdd = explicitAccount,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.account).isSameInstanceAs(explicitAccount)
        Truth.assertThat(result.network).isEqualTo(BITCOIN)
    }

    @Test
    fun `GIVEN accountToAdd is not available to add WHEN resolve THEN still use it`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val explicitAccount = availableAccount(isAvailableToAdd = false, availableToAddNetworks = setOf(ETHEREUM))

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to availableAccount()),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
            accountToAdd = explicitAccount,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.account).isSameInstanceAs(explicitAccount)
    }

    @Test
    fun `GIVEN accountToAdd with no matching ordered networks WHEN resolve THEN fall back to first ordered network`() =
        runTest {
            val walletId = UserWalletId(WALLET_ID_A)
            val userWallet = userWallet(walletId)
            val explicitAccount = availableAccount(availableToAddNetworks = setOf(ETHEREUM))

            val data = availableData(
                wallets = mapOf(
                    walletId to walletEntry(
                        userWallet = userWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to availableAccount()),
                    ),
                ),
            )

            val result = resolver.resolve(
                availableToAddData = data,
                orderedNetworks = listOf(BITCOIN),
                selectedWallet = userWallet,
                tokenParams = tokenParams,
                accountToAdd = explicitAccount,
            )

            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result!!.account).isSameInstanceAs(explicitAccount)
            Truth.assertThat(result.network).isEqualTo(BITCOIN)
        }

    @Test
    fun `GIVEN selected wallet is in data WHEN resolve THEN pick its entry`() = runTest {
        val selectedWalletId = UserWalletId(WALLET_ID_A)
        val otherWalletId = UserWalletId(WALLET_ID_B)
        val selectedUserWallet = userWallet(selectedWalletId)
        val otherUserWallet = userWallet(otherWalletId)

        val selectedAccount = availableAccount()
        val otherAccount = availableAccount()

        val data = availableData(
            wallets = linkedMapOf(
                otherWalletId to walletEntry(otherUserWallet, mapOf(AccountId.forMainCryptoPortfolio(otherWalletId) to otherAccount)),
                selectedWalletId to walletEntry(selectedUserWallet, mapOf(AccountId.forMainCryptoPortfolio(selectedWalletId) to selectedAccount)),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = selectedUserWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.userWallet).isSameInstanceAs(selectedUserWallet)
        Truth.assertThat(result.account).isSameInstanceAs(selectedAccount)
    }

    @Test
    fun `GIVEN selected wallet is not in data WHEN resolve THEN fall back to first wallet`() = runTest {
        val firstWalletId = UserWalletId(WALLET_ID_A)
        val firstUserWallet = userWallet(firstWalletId)
        val firstAccount = availableAccount()

        val data = availableData(
            wallets = mapOf(
                firstWalletId to walletEntry(firstUserWallet, mapOf(AccountId.forMainCryptoPortfolio(firstWalletId) to firstAccount)),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = userWallet(UserWalletId(WALLET_ID_B)),
            tokenParams = tokenParams,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.userWallet).isSameInstanceAs(firstUserWallet)
    }

    @Test
    fun `GIVEN selected wallet is null WHEN resolve THEN fall back to first wallet`() = runTest {
        val firstWalletId = UserWalletId(WALLET_ID_A)
        val firstUserWallet = userWallet(firstWalletId)
        val firstAccount = availableAccount()

        val data = availableData(
            wallets = mapOf(
                firstWalletId to walletEntry(firstUserWallet, mapOf(AccountId.forMainCryptoPortfolio(firstWalletId) to firstAccount)),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = null,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.userWallet).isSameInstanceAs(firstUserWallet)
    }

    @Test
    fun `GIVEN main account is available WHEN resolve THEN pick main account`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val mainAccount = availableAccount()
        val otherAccount = availableAccount()

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = linkedMapOf(
                        AccountId.forPaymentAccount(walletId) to otherAccount,
                        AccountId.forMainCryptoPortfolio(walletId) to mainAccount,
                    ),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.account).isSameInstanceAs(mainAccount)
    }

    @Test
    fun `GIVEN main account is not available WHEN resolve THEN pick first available account`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val mainAccount = availableAccount(isAvailableToAdd = false, availableToAddNetworks = emptySet())
        val secondAvailable = availableAccount()

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = linkedMapOf(
                        AccountId.forMainCryptoPortfolio(walletId) to mainAccount,
                        AccountId.forPaymentAccount(walletId) to secondAvailable,
                    ),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.account).isSameInstanceAs(secondAvailable)
    }

    @Test
    fun `GIVEN no available accounts WHEN resolve THEN fall back to that account with first ordered network`() =
        runTest {
            val walletId = UserWalletId(WALLET_ID_A)
            val userWallet = userWallet(walletId)
            val unavailable = availableAccount(isAvailableToAdd = false, availableToAddNetworks = emptySet())

            val data = availableData(
                wallets = mapOf(
                    walletId to walletEntry(
                        userWallet = userWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to unavailable),
                    ),
                ),
            )

            val result = resolver.resolve(
                availableToAddData = data,
                orderedNetworks = listOf(ETHEREUM),
                selectedWallet = userWallet,
                tokenParams = tokenParams,
            )

            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result!!.userWallet).isSameInstanceAs(userWallet)
            Truth.assertThat(result.account).isSameInstanceAs(unavailable)
            Truth.assertThat(result.network).isEqualTo(ETHEREUM)
        }

    @Test
    fun `GIVEN ordered networks do not match account networks WHEN resolve THEN fall back to first ordered network`() =
        runTest {
            val walletId = UserWalletId(WALLET_ID_A)
            val userWallet = userWallet(walletId)
            val account = availableAccount(availableToAddNetworks = setOf(ETHEREUM))

            val data = availableData(
                wallets = mapOf(
                    walletId to walletEntry(
                        userWallet = userWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to account),
                    ),
                ),
            )

            val result = resolver.resolve(
                availableToAddData = data,
                orderedNetworks = listOf(BITCOIN),
                selectedWallet = userWallet,
                tokenParams = tokenParams,
            )

            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result!!.userWallet).isSameInstanceAs(userWallet)
            Truth.assertThat(result.account).isSameInstanceAs(account)
            Truth.assertThat(result.network).isEqualTo(BITCOIN)
        }

    @Test
    fun `GIVEN preferred wallet has no available accounts but another wallet does WHEN resolve THEN pick the other`() =
        runTest {
            val preferredWalletId = UserWalletId(WALLET_ID_A)
            val otherWalletId = UserWalletId(WALLET_ID_B)
            val preferredUserWallet = userWallet(preferredWalletId)
            val otherUserWallet = userWallet(otherWalletId)

            val preferredUnavailable = availableAccount(isAvailableToAdd = false, availableToAddNetworks = emptySet())
            val otherAvailable = availableAccount(availableToAddNetworks = setOf(ETHEREUM))

            val data = availableData(
                wallets = linkedMapOf(
                    preferredWalletId to walletEntry(
                        userWallet = preferredUserWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(preferredWalletId) to preferredUnavailable),
                    ),
                    otherWalletId to walletEntry(
                        userWallet = otherUserWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(otherWalletId) to otherAvailable),
                    ),
                ),
            )

            val result = resolver.resolve(
                availableToAddData = data,
                orderedNetworks = listOf(ETHEREUM),
                selectedWallet = preferredUserWallet,
                tokenParams = tokenParams,
            )

            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result!!.userWallet).isSameInstanceAs(otherUserWallet)
            Truth.assertThat(result.account).isSameInstanceAs(otherAvailable)
            Truth.assertThat(result.network).isEqualTo(ETHEREUM)
        }

    @Test
    fun `GIVEN no wallet has viable combo WHEN resolve THEN fall back to preferred wallet with first ordered network`() =
        runTest {
            val preferredWalletId = UserWalletId(WALLET_ID_A)
            val otherWalletId = UserWalletId(WALLET_ID_B)
            val preferredUserWallet = userWallet(preferredWalletId)
            val otherUserWallet = userWallet(otherWalletId)

            val preferredUnavailable = availableAccount(isAvailableToAdd = false, availableToAddNetworks = emptySet())
            val otherUnavailable = availableAccount(isAvailableToAdd = false, availableToAddNetworks = emptySet())

            val data = availableData(
                wallets = linkedMapOf(
                    otherWalletId to walletEntry(
                        userWallet = otherUserWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(otherWalletId) to otherUnavailable),
                    ),
                    preferredWalletId to walletEntry(
                        userWallet = preferredUserWallet,
                        accounts = mapOf(AccountId.forMainCryptoPortfolio(preferredWalletId) to preferredUnavailable),
                    ),
                ),
            )

            val result = resolver.resolve(
                availableToAddData = data,
                orderedNetworks = listOf(ETHEREUM),
                selectedWallet = preferredUserWallet,
                tokenParams = tokenParams,
            )

            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result!!.userWallet).isSameInstanceAs(preferredUserWallet)
            Truth.assertThat(result.account).isSameInstanceAs(preferredUnavailable)
            Truth.assertThat(result.network).isEqualTo(ETHEREUM)
        }

    @Test
    fun `GIVEN first ordered network has derivation WHEN resolve THEN pick first`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val account = availableAccount(availableToAddNetworks = setOf(ETHEREUM, BITCOIN))

        val ethereumCurrency = cryptoCurrency()
        val bitcoinCurrency = cryptoCurrency()
        coEvery { getTokenMarketCryptoCurrency(any(), any(), ETHEREUM, any()) } returns ethereumCurrency
        coEvery { getTokenMarketCryptoCurrency(any(), any(), BITCOIN, any()) } returns bitcoinCurrency
        every { networkHasDerivationUseCase(any(), ethereumCurrency.network) } returns true.right()
        every { networkHasDerivationUseCase(any(), bitcoinCurrency.network) } returns true.right()

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to account),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM, BITCOIN),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result?.network).isEqualTo(ETHEREUM)
    }

    @Test
    fun `GIVEN only second ordered network has derivation WHEN resolve THEN pick second`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val account = availableAccount(availableToAddNetworks = setOf(ETHEREUM, BITCOIN))

        val ethereumCurrency = cryptoCurrency()
        val bitcoinCurrency = cryptoCurrency()
        coEvery { getTokenMarketCryptoCurrency(any(), any(), ETHEREUM, any()) } returns ethereumCurrency
        coEvery { getTokenMarketCryptoCurrency(any(), any(), BITCOIN, any()) } returns bitcoinCurrency
        every { networkHasDerivationUseCase(any(), ethereumCurrency.network) } returns false.right()
        every { networkHasDerivationUseCase(any(), bitcoinCurrency.network) } returns true.right()

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to account),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(ETHEREUM, BITCOIN),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result?.network).isEqualTo(BITCOIN)
    }

    @Test
    fun `GIVEN no network has derivation WHEN resolve THEN fall back to first ordered available network`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val account = availableAccount(availableToAddNetworks = setOf(ETHEREUM, BITCOIN))

        val ethereumCurrency = cryptoCurrency()
        val bitcoinCurrency = cryptoCurrency()
        coEvery { getTokenMarketCryptoCurrency(any(), any(), ETHEREUM, any()) } returns ethereumCurrency
        coEvery { getTokenMarketCryptoCurrency(any(), any(), BITCOIN, any()) } returns bitcoinCurrency
        every { networkHasDerivationUseCase(any(), any()) } returns Throwable().left()

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to account),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(BITCOIN, ETHEREUM),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result?.network).isEqualTo(BITCOIN)
    }

    @Test
    fun `GIVEN get token market crypto currency returns null WHEN resolve THEN fall back to first ordered available network`() = runTest {
        val walletId = UserWalletId(WALLET_ID_A)
        val userWallet = userWallet(walletId)
        val account = availableAccount(availableToAddNetworks = setOf(ETHEREUM, BITCOIN))

        coEvery { getTokenMarketCryptoCurrency(any(), any(), any(), any()) } returns null

        val data = availableData(
            wallets = mapOf(
                walletId to walletEntry(
                    userWallet = userWallet,
                    accounts = mapOf(AccountId.forMainCryptoPortfolio(walletId) to account),
                ),
            ),
        )

        val result = resolver.resolve(
            availableToAddData = data,
            orderedNetworks = listOf(BITCOIN, ETHEREUM),
            selectedWallet = userWallet,
            tokenParams = tokenParams,
        )

        Truth.assertThat(result?.network).isEqualTo(BITCOIN)
    }

    // region Helpers

    private fun availableData(wallets: Map<UserWalletId, AvailableToAddWallet>): AvailableToAddData = mockk {
        every { isAvailableToAdd } returns true
        every { availableToAddWallets } returns wallets
    }

    private fun walletEntry(
        userWallet: UserWallet,
        accounts: Map<AccountId, AvailableToAddAccount>,
    ): AvailableToAddWallet = mockk {
        every { this@mockk.userWallet } returns userWallet
        every { availableToAddAccounts } returns accounts
    }

    private fun availableAccount(
        isAvailableToAdd: Boolean = true,
        availableToAddNetworks: Set<TokenMarketInfo.Network> = setOf(ETHEREUM),
        derivationIndex: DerivationIndex = DerivationIndex.Main,
    ): AvailableToAddAccount = mockk {
        every { this@mockk.isAvailableToAdd } returns isAvailableToAdd
        every { this@mockk.availableToAddNetworks } returns availableToAddNetworks
        every { account.account.derivationIndex } returns derivationIndex
    }

    private fun userWallet(walletId: UserWalletId): UserWallet = mockk {
        every { this@mockk.walletId } returns walletId
    }

    private fun cryptoCurrency(): CryptoCurrency = mockk {
        every { network } returns mockk<Network>()
    }

    // endregion

    private companion object {
        const val WALLET_ID_A = "011f"
        const val WALLET_ID_B = "022e"

        val ETHEREUM = TokenMarketInfo.Network(
            networkId = "ethereum",
            isExchangeable = true,
            contractAddress = null,
            decimalCount = 18,
        )

        val BITCOIN = TokenMarketInfo.Network(
            networkId = "bitcoin",
            isExchangeable = true,
            contractAddress = null,
            decimalCount = 8,
        )
    }
}