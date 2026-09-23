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
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.eligibility.TangemPayCountrySelectorComponent
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.choice.TangemPayBankWithdrawChoiceComponent
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoiceConfig
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoices
import com.tangem.features.tangempay.withdraw.bank.form.TangemPayBankWithdrawFormComponent
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFlow
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.TangemPayBankWithdrawModel
import com.tangem.features.tangempay.withdraw.bank.model.cutToAmountThenPreview
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
import com.tangem.features.tangempay.withdraw.bank.recipients.TangemPayBankWithdrawRecipientsComponent
import com.tangem.features.tangempay.withdraw.bank.success.TangemPayBankWithdrawSuccessComponent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayBankWithdrawComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val countrySelectorFactory: TangemPayCountrySelectorComponent.Factory,
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
                onCloseFlow = ::closeFlow,
            ),
        )
        TangemPayBankWithdrawInnerRoute.Amount -> amountChild(
            step = config,
            componentContext = componentContext,
            returnToPreview = false,
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
        TangemPayBankWithdrawInnerRoute.RecipientDetails -> formChild(
            step = config,
            componentContext = componentContext,
            fields = recipientFields(),
            returnToPreview = false,
        )
        TangemPayBankWithdrawInnerRoute.BankDetails -> formChild(
            step = config,
            componentContext = componentContext,
            fields = BankWithdrawFormFields.bankDetails,
            returnToPreview = false,
        )
        TangemPayBankWithdrawInnerRoute.Preview -> TangemPayBankWithdrawPreviewComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankWithdrawPreviewComponent.Params(
                userWalletId = params.userWalletId,
                draft = model.draft,
                onOrderCreated = model::onOrderCreated,
                onOrderSigned = ::onOrderSigned,
            ),
        )
        TangemPayBankWithdrawInnerRoute.Success -> TangemPayBankWithdrawSuccessComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankWithdrawSuccessComponent.Params(
                draft = model.draft,
                onDoneClick = ::closeFlow,
            ),
        )
        is TangemPayBankWithdrawInnerRoute.Edit -> editChild(step = config, componentContext = componentContext)
    }

    private fun editChild(
        step: TangemPayBankWithdrawInnerRoute.Edit,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (step.target) {
        BankWithdrawEditTarget.Amount -> amountChild(
            step = step,
            componentContext = componentContext,
            returnToPreview = true,
        )
        BankWithdrawEditTarget.RecipientDetails -> formChild(
            step = step,
            componentContext = componentContext,
            fields = recipientFields(),
            returnToPreview = true,
        )
        BankWithdrawEditTarget.BankDetails -> formChild(
            step = step,
            componentContext = componentContext,
            fields = BankWithdrawFormFields.bankDetails,
            returnToPreview = true,
        )
    }

    private fun amountChild(
        step: TangemPayBankWithdrawInnerRoute,
        componentContext: ComponentContext,
        returnToPreview: Boolean,
    ): ComposableContentComponent = TangemPayBankWithdrawAmountComponent(
        appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        params = TangemPayBankWithdrawAmountComponent.Params(
            draft = model.draft,
            options = params.options,
            step = step,
            onContinue = { result ->
                model.onAmountConfirmed(result)
                onStepConfirmed(step = step, returnToPreview = returnToPreview)
            },
            onCloseFlow = ::closeFlow,
        ),
    )

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
            onCloseFlow = ::closeFlow,
        ),
    )

    private fun formChild(
        step: TangemPayBankWithdrawInnerRoute,
        componentContext: ComponentContext,
        fields: ImmutableList<BankWithdrawFormField>,
        returnToPreview: Boolean,
    ): ComposableContentComponent = TangemPayBankWithdrawFormComponent(
        appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        params = TangemPayBankWithdrawFormComponent.Params(
            draft = model.draft,
            step = step,
            fields = fields,
            savedAccounts = model.savedAccounts,
            onBack = model::onDetailsFilled,
            onContinue = { result ->
                model.onDetailsFilled(result)
                onStepConfirmed(step = step, returnToPreview = returnToPreview)
            },
            onSavedRecipientSelected = ::onSavedRecipientSelected,
            onCloseFlow = ::closeFlow,
        ),
        countrySelectorFactory = countrySelectorFactory,
    )

    private fun onSavedRecipientSelected(account: BankAccount) {
        model.onSavedRecipientSelected(account)
        stackNavigation.navigate { stack -> stack.cutToAmountThenPreview() }
    }

    private fun recipientFields(): ImmutableList<BankWithdrawFormField> = BankWithdrawFormFields.recipientDetails(
        isBusiness = params.entry == BankWithdrawEntry.NewBusiness,
    )

    private fun onOrderSigned() {
        model.onOrderSigned()
        stackNavigation.pushNew(TangemPayBankWithdrawInnerRoute.Success)
    }

    private fun closeFlow() {
        router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails)
    }

    private fun onStepConfirmed(step: TangemPayBankWithdrawInnerRoute, returnToPreview: Boolean) {
        if (returnToPreview) {
            stackNavigation.pop()
        } else {
            pushNext(from = step)
        }
    }

    private fun pushNext(from: TangemPayBankWithdrawInnerRoute) {
        val next = BankWithdrawFlow.nextStep(draft = model.draft.value, current = from) ?: return
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
        val options: StateFlow<BankWithdrawOptions>,
    )
}