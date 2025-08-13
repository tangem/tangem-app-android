package com.tangem.features.account.createedit.entity

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.account.AccountCreateEditComponent
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal class AccountCreateEditUMBuilder @Inject constructor(
    val params: AccountCreateEditComponent.Params,
) {

    private val accountColors = CryptoPortfolioIcon.Color.entries.toImmutableList()
    private val accountIcons = CryptoPortfolioIcon.Icon.entries.toImmutableList()
    private val createIcon = CryptoPortfolioIcon.ofDefaultCustomAccount()

    val toolbarTitle: TextReference
        get() = when (params) {
            is AccountCreateEditComponent.Params.Create -> resourceReference(R.string.account_form_title_create)
            is AccountCreateEditComponent.Params.Edit -> resourceReference(R.string.account_form_title_edit)
        }

    fun initAccountUM(onNameChange: (String) -> Unit): AccountCreateEditUM.Account {
        return when (params) {
            is AccountCreateEditComponent.Params.Create -> AccountCreateEditUM.Account(
                name = "",
                portfolioIcon = createIcon,
                derivationInfo = TextReference.EMPTY,
                inputPlaceholder = resourceReference(R.string.account_form_placeholder_new_account),
                onNameChange = onNameChange,
            )
            is AccountCreateEditComponent.Params.Edit -> AccountCreateEditUM.Account(
                name = params.account.name.value,
                portfolioIcon = params.account.portfolioIcon,
                derivationInfo = TextReference.EMPTY, // todo account use Account.CryptoPortfolio.derivationIndex ?
                inputPlaceholder = resourceReference(R.string.account_form_placeholder_edit_account),
                onNameChange = onNameChange,
            )
        }
    }

    fun initColorsUM(onColorSelect: (CryptoPortfolioIcon.Color) -> Unit): AccountCreateEditUM.Colors {
        val selected: CryptoPortfolioIcon.Color = when (params) {
            is AccountCreateEditComponent.Params.Create -> createIcon.color
            is AccountCreateEditComponent.Params.Edit -> params.account.portfolioIcon.color
        }
        return AccountCreateEditUM.Colors(
            selected = selected,
            list = accountColors,
            onColorSelect = onColorSelect,
        )
    }

    fun initIconsUM(onIconSelect: (CryptoPortfolioIcon.Icon) -> Unit): AccountCreateEditUM.Icons {
        val selected: CryptoPortfolioIcon.Icon = when (params) {
            is AccountCreateEditComponent.Params.Create -> createIcon.value
            is AccountCreateEditComponent.Params.Edit -> params.account.portfolioIcon.value
        }
        return AccountCreateEditUM.Icons(
            selected = selected,
            list = accountIcons,
            onIconSelect = onIconSelect,
        )
    }

    fun initButtonUM(onConfirmClick: () -> Unit): AccountCreateEditUM.Button {
        val text: TextReference = when (params) {
            is AccountCreateEditComponent.Params.Create -> resourceReference(R.string.account_form_create_button)
            is AccountCreateEditComponent.Params.Edit -> resourceReference(R.string.account_form_edit_button)
        }
        return AccountCreateEditUM.Button(
            isButtonEnabled = false,
            onConfirmClick = onConfirmClick,
            text = text,
        )
    }

    internal companion object {

        val Account.portfolioIcon: CryptoPortfolioIcon
            get() = when (this) {
                is Account.CryptoPortfolio -> this.icon
            }

        fun AccountCreateEditUM.updateColorSelect(color: CryptoPortfolioIcon.Color): AccountCreateEditUM {
            val newIcon = CryptoPortfolioIcon.ofCustomAccount(
                value = account.portfolioIcon.value,
                color = color,
            )
            return this.copy(
                account = this.account.copy(portfolioIcon = newIcon),
                colorsState = this.colorsState.copy(selected = color),
            )
        }

        fun AccountCreateEditUM.updateIconSelect(icon: CryptoPortfolioIcon.Icon): AccountCreateEditUM {
            val newIcon = CryptoPortfolioIcon.ofCustomAccount(
                value = icon,
                color = account.portfolioIcon.color,
            )
            return this.copy(
                account = this.account.copy(portfolioIcon = newIcon),
                iconsState = this.iconsState.copy(selected = icon),
            )
        }

        fun AccountCreateEditUM.updateName(name: String): AccountCreateEditUM {
            return this.copy(account = this.account.copy(name = name))
        }

        fun AccountCreateEditUM.updateButton(isButtonEnabled: Boolean): AccountCreateEditUM {
            return this.copy(buttonState = this.buttonState.copy(isButtonEnabled = isButtonEnabled))
        }
    }
}