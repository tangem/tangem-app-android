package com.tangem.feature.onboarding.presentation.wallet2.model

import android.content.Context
import androidx.annotation.StringRes

data class OnboardingDescription(
    @StringRes val titleRes: Int? = null,
    @StringRes val subTitleRes: Int? = null,
    val title: String? = null,
    val subTitle: String? = null,
) {
    fun hasTitle(): Boolean = title != null || titleRes != null

    fun hasSubTitle(): Boolean = subTitle != null || subTitleRes != null

    fun getTitle(context: Context): String = context.get(titleRes, title)

    fun getSubTitle(context: Context): String = context.get(subTitleRes, subTitle)

    private fun Context.get(@StringRes resId: Int?, text: String?): String {
        return resId?.let { this.getString(it) } ?: text ?: ""
    }
}