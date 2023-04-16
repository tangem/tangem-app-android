package com.tangem.feature.onboarding.presentation.wallet2.viewmodel

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.domain.SeedPhraseError

/**
[REDACTED_AUTHOR]
 */
class SeedPhraseErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<SeedPhraseError?, String?> {

    override fun convert(message: SeedPhraseError?): String? {
        message ?: return null

        val convertedMessage = when (message) {
            SeedPhraseError.InvalidEntropyLength -> {
                context.getString(R.string.onboarding_seed_mnemonic_invalid_checksum)
            }
            is SeedPhraseError.InvalidWords -> {
                context.getString(R.string.onboarding_seed_mnemonic_wrong_words)
            }
            else -> null
        }

        return convertedMessage
    }
}