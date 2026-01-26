package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.style.TextDecoration
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class KycRejectedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<KycRejectedComponent.Params>()

    val uiState: StateFlow<MessageBottomSheetUMV2>
        field = MutableStateFlow(getInitialState())

    private fun getInitialState(): MessageBottomSheetUMV2 {
        return bottomSheetMessage {
            infoBlock {
                icon(com.tangem.core.ui.R.drawable.ic_heart_broken_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Warning
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Warning
                }
                title = resourceReference(R.string.tangempay_kyc_rejected)
                body = combinedReference(
                    resourceReference(R.string.tangempay_kyc_rejected_description),
                    stringReference(" "),
                    styledResourceReference(
                        id = R.string.tangempay_kyc_rejected_description_span,
                        spanStyleReference = {
                            TangemTheme.typography.body2.copy(TangemTheme.colors.text.accent).toSpanStyle()
                                .copy(textDecoration = TextDecoration.None)
                        },
                        onClick = {
                            params.callbacks.onClickYourProfile(userWalletId = params.walletId)
                            onDismiss()
                        },
                    ),
                )
            }
            primaryButton {
                text = resourceReference(R.string.tangempay_go_to_support)
                onClick = {
                    params.callbacks.onClickGoToSupport(customerId = params.customerId)
                    onDismiss()
                }
            }
            secondaryButton {
                text = resourceReference(R.string.tangempay_kyc_rejected_button_text)
                onClick = {
                    params.callbacks.onClickHideKyc(userWalletId = params.walletId)
                    onDismiss()
                }
            }
        }.messageBottomSheetUMV2
    }

    fun onDismiss() {
        params.onDismiss()
    }
}