package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import android.net.Uri
import com.tangem.utils.SupportedLanguages
import java.util.Locale

// TODO: [REDACTED_JIRA]
internal fun seedPhraseLearnMoreUrl(): String {
    val language = Locale.getDefault().language

    val languageBy = "by"
    if (SupportedLanguages.RUSSIAN.equals(language, true) ||
        languageBy.equals(language, true)
    ) {
        SupportedLanguages.RUSSIAN
    } else {
        SupportedLanguages.ENGLISH
    }

    val webUri = Uri.Builder()
        .scheme("https")
        .authority("tangem.com")
        .appendPath(language)
        .appendPath("blog/post/seed-phrase-a-risky-solution")
        .build()

    return webUri.toString()
}