package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountListConverterTest {

    private val userWallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("011")
    }
    private val cryptoPortfolioConverter = mockk<CryptoPortfolioConverter>()

    // Stubbed where it is declared: the converter resolves the factory in its constructor
    private val cryptoPortfolioConverterFactory = mockk<CryptoPortfolioConverter.Factory> {
        every { create(userWallet) } returns cryptoPortfolioConverter
    }
    private val collectiblesFeatureToggles = mockk<CollectiblesFeatureToggles>()
    private val jointAccountFeatureToggles = mockk<JointAccountFeatureToggles>()
    private val converter = AccountListConverter(
        userWallet = userWallet,
        cryptoPortfolioConverterFactory = cryptoPortfolioConverterFactory,
        collectiblesFeatureToggles = collectiblesFeatureToggles,
        jointAccountFeatureToggles = jointAccountFeatureToggles,
    )

    @BeforeEach
    fun setupEach() {
        clearMocks(cryptoPortfolioConverter, collectiblesFeatureToggles, jointAccountFeatureToggles)
        every { collectiblesFeatureToggles.isCollectiblesEnabled } returns false
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `cryptoPortfolioConverter throws exception`() {
            // Arrange
            val dto = createGetWalletAccountsResponse(userWallet.walletId)
            val exception = IllegalStateException("Test exception")

            every { cryptoPortfolioConverter.convert(any()) } throws exception

            // Act
            val actual = runCatching { converter.convert(dto) }.exceptionOrNull()!!

            // Asset
            val expected = exception
            Truth.assertThat(actual).isInstanceOf(expected::class.java)
            Truth.assertThat(actual.message).isEqualTo(expected.message)
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN a collectibles record WHEN convert THEN it becomes the collectibles account only when enabled`(
            isCollectiblesEnabled: Boolean,
        ) {
            // Arrange
            every { collectiblesFeatureToggles.isCollectiblesEnabled } returns isCollectiblesEnabled
            val response = createGetWalletAccountsResponse(userWallet.walletId)
            val cryptoRow = response.accounts.single()
            val collectiblesRow = createWalletAccountDTO(
                userWalletId = userWallet.walletId,
                accountId = "5A0E8F2C7B9D4163A8E1C3F5B7D9024E6A8C0E2F4B6D8A0C2E4F6A8B0C2D4E6F",
                derivationIndex = 999799,
                tokens = null,
                type = WalletAccountDTO.Type.COLLECTIBLES,
            )
            val mainAccount = Account.Personal.createMainAccount(userWalletId = userWallet.walletId)
            every { cryptoPortfolioConverter.convert(cryptoRow) } returns mainAccount

            // Act
            val actual = converter.convert(response.copy(accounts = listOf(cryptoRow, collectiblesRow)))

            // Assert
            val expected = listOfNotNull(
                mainAccount,
                Account.Collectibles(userWalletId = userWallet.walletId).takeIf { isCollectiblesEnabled },
            )
            Truth.assertThat(actual.accounts).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `GIVEN collectibles enabled and no record WHEN convert THEN there is no collectibles account`() {
            // Arrange
            every { collectiblesFeatureToggles.isCollectiblesEnabled } returns true
            val response = createGetWalletAccountsResponse(userWallet.walletId)
            val mainAccount = Account.Personal.createMainAccount(userWalletId = userWallet.walletId)
            every { cryptoPortfolioConverter.convert(response.accounts.single()) } returns mainAccount

            // Act
            val actual = converter.convert(response)

            // Assert
            Truth.assertThat(actual.accounts).containsExactly(mainAccount)
        }

        /** `api/v2` is in use for collectibles, so the joint rows arrive although joint accounts are off */
        @Test
        fun `GIVEN joint accounts disabled WHEN convert THEN joint records are left out`() {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
            val response = createGetWalletAccountsResponse(userWallet.walletId)
            val cryptoRow = response.accounts.single()
            val jointRow = createWalletAccountDTO(
                userWalletId = userWallet.walletId,
                accountId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                derivationIndex = 0,
                tokens = null,
                type = WalletAccountDTO.Type.JOINT,
            )
            val mainAccount = Account.Personal.createMainAccount(userWalletId = userWallet.walletId)
            every { cryptoPortfolioConverter.convert(cryptoRow) } returns mainAccount
            val withJoint = response.copy(
                wallet = response.wallet.copy(totalJointAccounts = 1),
                accounts = listOf(cryptoRow, jointRow),
            )

            // Act
            val actual = converter.convert(withJoint)

            // Assert
            Truth.assertThat(actual.accounts).containsExactly(mainAccount)
            Truth.assertThat(actual.totalJointAccounts).isEqualTo(0)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Arrange
            if (model.expected.isSuccess) {
                model.value.accounts.forEach { dto ->
                    val account = model.expected.getOrNull()!!.accounts
                        .firstOrNull { it.accountId.value == dto.id } as? Account.Personal

                    every { cryptoPortfolioConverter.convert(dto) } returns account!!
                }
            }

            // Act
            val actual = runCatching { converter.convert(model.value) }

            // Asset
            actual
                .onSuccess {
                    val expected = model.expected.getOrNull()
                    Truth.assertThat(it).isEqualTo(expected)
                }
                .onFailure {
                    val expected = model.expected.exceptionOrNull() ?: throw it
                    Truth.assertThat(it).isInstanceOf(expected::class.java)
                    Truth.assertThat(it.message).isEqualTo(expected.message)
                }
        }

        private fun provideTestModels(): List<ConvertModel> {
            return listOf(
                ConvertModel(
                    value = createGetWalletAccountsResponse(
                        userWalletId = userWallet.walletId,
                        sortType = UserTokensResponse.SortType.BALANCE,
                        groupType = UserTokensResponse.GroupType.NETWORK,
                    ),
                    expected = Result.success(
                        createAccountList(
                            userWalletId = userWallet.walletId,
                            sortType = TokensSortType.BALANCE,
                            groupType = TokensGroupType.NETWORK,
                        ),
                    ),
                ),
                ConvertModel(
                    value = createGetWalletAccountsResponse(
                        userWalletId = userWallet.walletId,
                        sortType = UserTokensResponse.SortType.MANUAL,
                        groupType = UserTokensResponse.GroupType.TOKEN,
                    ),
                    expected = Result.success(
                        createAccountList(
                            userWalletId = userWallet.walletId,
                            sortType = TokensSortType.NONE,
                            groupType = TokensGroupType.NONE,
                        ),
                    ),
                ),
                ConvertModel(
                    value = createGetWalletAccountsResponse(
                        userWalletId = userWallet.walletId,
                        sortType = UserTokensResponse.SortType.MARKETCAP,
                        groupType = UserTokensResponse.GroupType.NONE,
                    ),
                    expected = Result.success(
                        createAccountList(
                            userWalletId = userWallet.walletId,
                            sortType = TokensSortType.NONE,
                            groupType = TokensGroupType.NONE,
                        ),
                    ),
                ),
                ConvertModel(
                    value = GetWalletAccountsResponse(
                        wallet = GetWalletAccountsResponse.Wallet(
                            version = 0,
                            group = UserTokensResponse.GroupType.NETWORK,
                            sort = UserTokensResponse.SortType.BALANCE,
                            totalAccounts = 1,
                            totalArchivedAccounts = 0,
                        ),
                        accounts = emptyList(),
                        unassignedTokens = emptyList(),
                    ),
                    expected = Result.failure(
                        IllegalStateException(
                            "Failed to convert GetWalletAccountsResponse to AccountList: EmptyAccountsList: " +
                                "The accounts list cannot be empty",
                        ),
                    ),
                ),
            )
        }
    }

    data class ConvertModel(
        val value: GetWalletAccountsResponse,
        val expected: Result<AccountList>,
    )
}