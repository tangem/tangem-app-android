package com.tangem.features.collectibles.impl.onboarding.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_wallet_24
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class CollectiblesOnboardingUMConverter :
    Converter<CollectiblesOnboardingUMConverter.Callbacks, CollectiblesOnboardingUM> {

    override fun convert(value: Callbacks): CollectiblesOnboardingUM {
        return CollectiblesOnboardingUM(
            title = TextReference.Res(R.string.collectibles_welcome_title),
            subtitle = TextReference.Res(R.string.collectibles_welcome_description),
            benefits = persistentListOf(
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_lightning_24,
                    title = TextReference.Res(R.string.collectibles_welcome_benefit_tapless_title),
                    subtitle = TextReference.Res(R.string.collectibles_welcome_benefit_tapless_subtitle),
                ),
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_shield_checkmark_24,
                    title = TextReference.Res(R.string.collectibles_welcome_benefit_cold_wallet_title),
                    subtitle = TextReference.Res(R.string.collectibles_welcome_benefit_cold_wallet_subtitle),
                ),
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_shield_checkmark_24,
                    title = TextReference.Res(R.string.collectibles_welcome_benefit_transparency_title),
                    subtitle = TextReference.Res(R.string.collectibles_welcome_benefit_transparency_subtitle),
                ),
                CollectiblesOnboardingUM.Benefit(
                    icon = Icons.ic_wallet_24,
                    title = TextReference.Res(R.string.collectibles_welcome_benefit_sell_offers_title),
                    subtitle = TextReference.Res(R.string.collectibles_welcome_benefit_sell_offers_subtitle),
                ),
            ),
            faq = persistentListOf(
                CollectiblesOnboardingUM.FaqEntry(
                    question = TextReference.Res(R.string.collectibles_welcome_faq_tap_question),
                    answer = TextReference.Res(R.string.collectibles_welcome_faq_tap_answer),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = TextReference.Res(R.string.collectibles_welcome_faq_buying_question),
                    answer = TextReference.Res(R.string.collectibles_welcome_faq_buying_answer),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = TextReference.Res(R.string.collectibles_welcome_faq_risks_question),
                    answer = TextReference.Res(R.string.collectibles_welcome_faq_risks_answer),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = TextReference.Res(R.string.collectibles_welcome_faq_provider_question),
                    answer = TextReference.Res(R.string.collectibles_welcome_faq_provider_answer),
                ),
                CollectiblesOnboardingUM.FaqEntry(
                    question = TextReference.Res(R.string.collectibles_welcome_faq_availability_question),
                    answer = TextReference.Res(R.string.collectibles_welcome_faq_availability_answer),
                ),
            ),
            collectiblesTitle = TextReference.Res(R.string.collectibles_welcome_legal_collector_crypt_terms),
            tangemTitle = resourceReference(R.string.collectibles_welcome_legal_tangem_terms),
            privacyTitle = TextReference.Res(R.string.collectibles_welcome_legal_privacy_notice),
            createAccountButtonText = TextReference.Res(R.string.collectibles_welcome_create_account_button),
            isCreatingAccount = false,
            onCloseClick = value.onCloseClick,
            onCreateAccountClick = value.onCreateAccountClick,
            onCollectiblesTermsClick = value.onCollectiblesTermsClick,
            onTangemTermsClick = value.onTangemTermsClick,
            onPrivacyNoticeClick = value.onPrivacyNoticeClick,
        )
    }

    data class Callbacks(
        val onCloseClick: () -> Unit,
        val onCreateAccountClick: () -> Unit,
        val onCollectiblesTermsClick: () -> Unit,
        val onTangemTermsClick: () -> Unit,
        val onPrivacyNoticeClick: () -> Unit,
    )
}