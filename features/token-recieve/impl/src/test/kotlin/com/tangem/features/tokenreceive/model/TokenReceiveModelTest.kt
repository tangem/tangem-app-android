package com.tangem.features.tokenreceive.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.tokenreceive.TokenReceiveFeatureToggles
import com.tangem.features.tokenreceive.experiment.GetAddFundsUpdateVariantUseCase
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenReceiveModelTest {

    private val featureToggles: TokenReceiveFeatureToggles = mockk()
    private val abTestsManager: ABTestsManager = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureToggles, abTestsManager)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN toggle entry point and variant WHEN model created THEN new screen shown only for treatment`(
        model: ScreenModel,
    ) {
        // Arrange
        every { featureToggles.isNewReceiveScreenEnabled } returns model.isToggleEnabled
        every { abTestsManager.getCachedValue(EXPERIMENT_KEY) } returns model.cachedVariant

        // Act
        val receiveModel = createModel(isNewScreenAllowed = model.isNewScreenAllowed)

        // Assert
        assertThat(receiveModel.isNewScreenShown).isEqualTo(model.expectedNewScreen)
        verify(exactly = model.expectedVariantReads) { abTestsManager.getCachedValue(EXPERIMENT_KEY) }
        receiveModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        ScreenModel(
            isToggleEnabled = true,
            isNewScreenAllowed = true,
            cachedVariant = "treatment",
            expectedNewScreen = true,
            expectedVariantReads = 1,
        ),
        ScreenModel(
            isToggleEnabled = true,
            isNewScreenAllowed = true,
            cachedVariant = "control",
            expectedNewScreen = false,
            expectedVariantReads = 1,
        ),
        ScreenModel(
            isToggleEnabled = true,
            isNewScreenAllowed = true,
            cachedVariant = null,
            expectedNewScreen = false,
            expectedVariantReads = 1,
        ),
        ScreenModel(
            isToggleEnabled = false,
            isNewScreenAllowed = true,
            cachedVariant = "treatment",
            expectedNewScreen = false,
            expectedVariantReads = 0,
        ),
        ScreenModel(
            isToggleEnabled = true,
            isNewScreenAllowed = false,
            cachedVariant = "treatment",
            expectedNewScreen = false,
            expectedVariantReads = 0,
        ),
    )

    data class ScreenModel(
        val isToggleEnabled: Boolean,
        val isNewScreenAllowed: Boolean,
        val cachedVariant: String?,
        val expectedNewScreen: Boolean,
        val expectedVariantReads: Int,
    )

    private fun createModel(isNewScreenAllowed: Boolean) = TokenReceiveModel(
        clipboardManager = mockk(),
        shareManager = mockk(),
        getReverseResolvedEnsAddressUseCase = mockk(),
        saveViewedTokenReceiveWarningUseCase = mockk(),
        paramsContainer = MutableParamsContainer(
            TokenReceiveComponent.Params(
                config = TokenReceiveConfig(
                    shouldShowWarning = false,
                    cryptoCurrency = createBitcoin(),
                    userWalletId = UserWalletId("011"),
                    shouldShowMemoDisclaimer = false,
                    receiveAddress = emptyList(),
                ),
                onDismiss = {},
                isNewScreenAllowed = isNewScreenAllowed,
            ),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = mockk(),
        featureToggles = featureToggles,
        getAddFundsUpdateVariant = GetAddFundsUpdateVariantUseCase(abTestsManager),
    )

    private fun createBitcoin(): CryptoCurrency.Coin {
        val blockchain = Blockchain.Bitcoin
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(blockchain.toNetworkId()),
                suffix = CryptoCurrency.ID.Suffix.RawID(blockchain.id),
            ),
            network = Network(
                id = Network.ID(value = blockchain.toNetworkId(), derivationPath = Network.DerivationPath.None),
                name = blockchain.fullName,
                currencySymbol = blockchain.currency,
                derivationPath = Network.DerivationPath.None,
                isTestnet = false,
                standardType = Network.StandardType.Unspecified(blockchain.name),
                hasFiatFeeRate = true,
                canHandleTokens = false,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = blockchain.fullName,
            symbol = blockchain.currency,
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )
    }

    private companion object {
        const val EXPERIMENT_KEY = "twi_1958_add_funds_update"
    }
}