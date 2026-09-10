package com.tangem.features.tangempay.withdraw.bank

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.choice.TangemPayBankWithdrawChoiceComponent
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoiceConfig
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoices
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFlow
import com.tangem.features.tangempay.withdraw.bank.model.TangemPayBankWithdrawModel
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.recipients.TangemPayBankWithdrawRecipientsComponent
import com.tangem.features.tangempay.withdraw.bank.stub.TangemPayBankWithdrawStepStubComponent

internal class TangemPayBankWithdrawComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankWithdrawModel = getOrCreateModel(params = params)

    private val stackNavigation = StackNavigation<TangemPayBankWithdrawInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayBankWithdrawInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayBankWithdrawInnerStack",
        source = stackNavigation,
        serializer = TangemPayBankWithdrawInnerRoute.serializer(),
        initialConfiguration = BankWithdrawFlow.initialStep(params.entry),
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by childStack.subscribeAsState()
        BackHandler(onBack = ::onChildBack)
        Children(modifier = modifier, stack = stack) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun screenChild(
        config: TangemPayBankWithdrawInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayBankWithdrawInnerRoute.Recipients -> TangemPayBankWithdrawRecipientsComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankWithdrawRecipientsComponent.Params(
                userWalletId = params.userWalletId,
                onRecipientSelected = { account ->
                    model.onRecipientSelected(account)
                    pushNext(from = config)
                },
            ),
        )
        TangemPayBankWithdrawInnerRoute.Amount -> TangemPayBankWithdrawAmountComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankWithdrawAmountComponent.Params(
                userWalletId = params.userWalletId,
                draft = model.draft,
                onContinue = { result ->
                    model.onAmountConfirmed(result)
                    pushNext(from = config)
                },
            ),
        )
        TangemPayBankWithdrawInnerRoute.TransferType -> choiceChild(
            step = config,
            componentContext = componentContext,
            choice = BankWithdrawChoices.transferType(isThirdParty = model.draft.value.newRecipient.isThirdParty),
            onSelected = { selectedId ->
                model.onTransferTypeSelected(isThirdParty = BankWithdrawChoices.isThirdParty(selectedId))
            },
        )
        TangemPayBankWithdrawInnerRoute.AccountType -> choiceChild(
            step = config,
            componentContext = componentContext,
            choice = BankWithdrawChoices.accountType(selected = model.draft.value.newRecipient.accountType),
            onSelected = { selectedId -> model.onAccountTypeSelected(BankWithdrawChoices.accountTypeOf(selectedId)) },
        )
        TangemPayBankWithdrawInnerRoute.RecipientDetails,
        TangemPayBankWithdrawInnerRoute.BankDetails,
        TangemPayBankWithdrawInnerRoute.Preview,
        -> TangemPayBankWithdrawStepStubComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankWithdrawStepStubComponent.Params(draft = model.draft, step = config),
        )
    }

    private fun choiceChild(
        step: TangemPayBankWithdrawInnerRoute,
        componentContext: ComponentContext,
        choice: BankWithdrawChoiceConfig,
        onSelected: (selectedId: String) -> Unit,
    ): ComposableContentComponent = TangemPayBankWithdrawChoiceComponent(
        appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        params = TangemPayBankWithdrawChoiceComponent.Params(
            draft = model.draft,
            step = step,
            config = choice,
            onContinue = { selectedId ->
                onSelected(selectedId)
                pushNext(from = step)
            },
        ),
    )

    private fun pushNext(from: TangemPayBankWithdrawInnerRoute) {
        val next = BankWithdrawFlow.nextStep(entry = params.entry, current = from) ?: return
        stackNavigation.pushNew(next)
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    data class Params(
        val userWalletId: UserWalletId,
        val entry: BankWithdrawEntry,
    )
}