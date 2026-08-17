package com.tangem.features.jointaccount.main.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.jointaccount.main.JointAccountMainBlockComponent
import com.tangem.features.jointaccount.main.JointAccountMainUM
import com.tangem.features.jointaccount.main.ui.JointAccountMainBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val JOINT_ACCOUNT_CONTENT_TYPE = "JointAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultJointAccountMainBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : JointAccountMainBlockComponent, AppComponentContext by context {

    override fun LazyListScope.jointAccountMainContent(
        key: String,
        state: JointAccountMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        item(
            key = key,
            contentType = JOINT_ACCOUNT_CONTENT_TYPE,
        ) {
            JointAccountMainBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.animateContentSize(),
            )
        }
    }

    @AssistedFactory
    interface Factory : JointAccountMainBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultJointAccountMainBlockComponent
    }
}