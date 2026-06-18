package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessageButtonUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.domain.WalletCardWarning
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.res.R as CoreResR

/**
 * Maps card/wallet-level warnings (produced by `GetWalletCardWarningsUseCase` for a single-currency wallet) into
 * Token Details notifications and prepends them to the existing currency-level notifications.
 */
internal class UpdateWalletCardWarningsTransformer(
    private val walletCardWarnings: Set<WalletCardWarning>,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val others = prevState.notifications.filterNot { it.id in WALLET_CARD_WARNING_IDS }
        val messages = walletCardWarnings.map(::mapWarning)

        return prevState.copy(
            notifications = (messages + others).toImmutableList(),
        )
    }

    @Suppress("LongMethod")
    private fun mapWarning(warning: WalletCardWarning): TangemMessageUM {
        return when (warning) {
            WalletCardWarning.BackupError -> TangemMessageUM(
                id = ID_BACKUP_ERROR,
                title = resourceReference(CoreResR.string.warning_backup_errors_title),
                subtitle = resourceReference(CoreResR.string.warning_backup_errors_message),
                messageEffect = TangemMessageEffect.Warning,
                iconUM = attentionIcon(),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.common_contact_support),
                        type = TangemButtonType.Secondary,
                        onClick = clickIntents::onSupportClick,
                    ),
                ),
            )
            WalletCardWarning.DevCard -> TangemMessageUM(
                id = ID_DEV_CARD,
                title = resourceReference(CoreResR.string.warning_developer_card_title),
                subtitle = resourceReference(CoreResR.string.warning_developer_card_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = attentionIcon(),
            )
            WalletCardWarning.FailedCardValidation -> TangemMessageUM(
                id = ID_FAILED_CARD_VALIDATION,
                title = resourceReference(CoreResR.string.warning_failed_to_verify_card_title),
                subtitle = resourceReference(CoreResR.string.warning_failed_to_verify_card_message),
                messageEffect = TangemMessageEffect.Warning,
                iconUM = attentionIcon(),
            )
            WalletCardWarning.TestnetCard -> TangemMessageUM(
                id = ID_TESTNET_CARD,
                title = resourceReference(CoreResR.string.warning_testnet_card_title),
                subtitle = resourceReference(CoreResR.string.warning_testnet_card_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = attentionIcon(),
            )
            is WalletCardWarning.LowSignatures -> TangemMessageUM(
                id = ID_LOW_SIGNATURES,
                title = resourceReference(CoreResR.string.warning_low_signatures_title),
                subtitle = resourceReference(
                    id = CoreResR.string.warning_low_signatures_message,
                    formatArgs = wrappedList(warning.count.toString()),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = attentionIcon(),
            )
            WalletCardWarning.NumberOfSignedHashesIncorrect -> TangemMessageUM(
                id = ID_NUMBER_OF_SIGNED_HASHES_INCORRECT,
                title = resourceReference(CoreResR.string.warning_number_of_signed_hashes_incorrect_title),
                subtitle = resourceReference(CoreResR.string.warning_number_of_signed_hashes_incorrect_message),
                messageEffect = TangemMessageEffect.Warning,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.img_knight_shield_32,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                onCloseClick = clickIntents::onCloseSignedHashesWarning,
            )
            WalletCardWarning.DemoCard -> TangemMessageUM(
                id = ID_DEMO_CARD,
                title = resourceReference(CoreResR.string.warning_demo_mode_title),
                subtitle = resourceReference(CoreResR.string.warning_demo_mode_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = attentionIcon(),
            )
        }
    }

    private fun attentionIcon(): TangemIconUM.Icon = TangemIconUM.Icon(
        iconRes = R.drawable.ic_attention_default_24,
        tintReference = { TangemTheme.colors2.graphic.neutral.primary },
    )

    private companion object {
        const val ID_BACKUP_ERROR = "BackupErrorNotification"
        const val ID_DEV_CARD = "DevCardNotification"
        const val ID_FAILED_CARD_VALIDATION = "FailedCardValidationNotification"
        const val ID_TESTNET_CARD = "TestnetCardNotification"
        const val ID_LOW_SIGNATURES = "LowSignaturesNotification"
        const val ID_NUMBER_OF_SIGNED_HASHES_INCORRECT = "NumberOfSignedHashesIncorrectNotification"
        const val ID_DEMO_CARD = "DemoCardNotification"

        val WALLET_CARD_WARNING_IDS = setOf(
            ID_BACKUP_ERROR,
            ID_DEV_CARD,
            ID_FAILED_CARD_VALIDATION,
            ID_TESTNET_CARD,
            ID_LOW_SIGNATURES,
            ID_NUMBER_OF_SIGNED_HASHES_INCORRECT,
            ID_DEMO_CARD,
        )
    }
}