package com.tangem.core.ui.ds2.modal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds2.modalinformer.TangemModalInformer
import com.tangem.core.ui.ds2.modalmenu.TangemModalMenu
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus

/**
 * Renders any [TangemModalContentUM] with its template: [TangemModalStatus], [TangemModalInformer] or
 * [TangemModalMenu].
 *
 * @param state Content to render.
 * @param actionScope Receiver of the content's click actions — the menu rows; lets them close the modal.
 * @param modifier Modifier for the template's root container.
 */
@Composable
fun TangemModalContent(
    state: TangemModalContentUM,
    actionScope: TangemModalActionScope,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemModalContentUM.Status -> TangemModalStatus(state = state, modifier = modifier)
        is TangemModalContentUM.Informer -> TangemModalInformer(state = state, modifier = modifier)
        is TangemModalContentUM.Menu -> TangemModalMenu(state = state, actionScope = actionScope, modifier = modifier)
    }
}