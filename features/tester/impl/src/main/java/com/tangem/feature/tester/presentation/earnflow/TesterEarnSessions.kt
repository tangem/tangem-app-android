package com.tangem.feature.tester.presentation.earnflow

import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.flow.api.EarnFlowLegal
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.impl.fake.FakeEarnSessionBehavior
import com.tangem.features.earn.impl.fake.FakeEarnSessionPreset
import com.tangem.features.earn.impl.fake.createFakeEarnSession
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnProductKey
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.session.EarnSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Singleton
import com.tangem.core.ui.R as CoreUiR

/** Read when the flow creates its session, so the switches apply to the next opened flow. */
@Singleton
internal class TesterEarnSessionSettings @Inject constructor() {

    @Volatile
    var behavior: FakeEarnSessionBehavior = FakeEarnSessionBehavior()
}

/**
 * The fake sessions live under their own product ids: the real products will register `lending` and `vault`, and a
 * second binding for the same key would not compile.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TesterEarnSessionsModule {

    @Provides
    @IntoMap
    @EarnProductKey(TesterEarnProducts.LENDING_ID)
    fun provideLendingSession(settings: TesterEarnSessionSettings): EarnSession.Factory =
        EarnSession.Factory { createFakeEarnSession(FakeEarnSessionPreset.Lending, settings.behavior) }

    @Provides
    @IntoMap
    @EarnProductKey(TesterEarnProducts.VAULT_ID)
    fun provideVaultSession(settings: TesterEarnSessionSettings): EarnSession.Factory =
        EarnSession.Factory { createFakeEarnSession(FakeEarnSessionPreset.Vault, settings.behavior) }
}

internal object TesterEarnProducts {

    const val LENDING_ID = "tester_fake_lending"
    const val VAULT_ID = "tester_fake_vault"

    fun flowProduct(preset: FakeEarnSessionPreset): EarnFlowProduct = when (preset) {
        FakeEarnSessionPreset.Lending -> EarnFlowProduct(
            productId = EarnProductId(LENDING_ID),
            title = stringReference("Lending on Aave"),
            provider = EarnProvider(
                title = stringReference("Aave"),
                icon = ImageReference.Res(CoreUiR.drawable.ic_aave_36),
            ),
            analytics = EarnProductAnalytics(category = "Earn", product = "Lending"),
            howItWorks = howItWorks(provider = "Aave", auditor = "Pessimistic"),
            legal = TESTER_LEGAL,
        )
        FakeEarnSessionPreset.Vault -> EarnFlowProduct(
            productId = EarnProductId(VAULT_ID),
            title = stringReference("High Yield Vault"),
            provider = EarnProvider(
                title = stringReference("Gauntlet"),
                icon = ImageReference.Res(CoreUiR.drawable.ic_aave_36),
            ),
            analytics = EarnProductAnalytics(category = "Earn", product = "Vault"),
            howItWorks = howItWorks(provider = "Gauntlet", auditor = "Spearbit"),
            legal = TESTER_LEGAL,
        )
    }

    private fun howItWorks(provider: String, auditor: String) = EarnHowItWorks(
        title = stringReference("How it works"),
        entries = listOf(
            EarnTextBlock(
                title = stringReference("You choose the amount"),
                description = stringReference("Pick how much ETH to lend. The rest stays in your wallet, untouched."),
            ),
            EarnTextBlock(
                title = stringReference("Your money starts working"),
                description = stringReference("Any interest you receive is compounded. Nothing to manage"),
            ),
            EarnTextBlock(
                title = stringReference("Take it back anytime"),
                description = stringReference(
                    "Withdrawals are usually instant, subject to available $provider liquidity.",
                ),
            ),
        ),
        footnote = stringReference("Contract audited by $auditor"),
        learnMoreUrl = "https://tangem.com",
    )
}

private val TESTER_LEGAL = EarnFlowLegal(
    termsOfUseUrl = "https://tangem.com/tangem_tos.html",
    privacyPolicyUrl = "https://tangem.com/privacy-policy.html",
)