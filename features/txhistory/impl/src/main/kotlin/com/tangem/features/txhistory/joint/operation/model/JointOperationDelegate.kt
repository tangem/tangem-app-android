package com.tangem.features.txhistory.joint.operation.model

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.AddressBookOpenMode
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.addressbook.usecase.IsAddressBookCompatibleUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.RejectJointOperationUseCase
import com.tangem.domain.jointaccount.usecase.SignJointOperationUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.joint.ownJointAccount
import com.tangem.features.txhistory.model.ContactCandidate
import com.tangem.features.txhistory.model.TxHistoryOwnerLookupProducer
import com.tangem.features.txhistory.model.isSavableAsContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * An operation is exposed while the backend lists it: awaiting signatures, or past the threshold and on its way to
 * the network. A status this build does not know is hidden rather than guessed at.
 */
@Suppress("LongParameterList")
internal class JointOperationDelegate @Inject constructor(
    private val router: Router,
    private val accountListSupplier: SingleAccountListSupplier,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val transferResolver: JointOperationTransferResolver,
    private val signersResolver: JointOperationSignersResolver,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val signJointOperationUseCase: SignJointOperationUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val rejectJointOperationUseCase: RejectJointOperationUseCase,
    private val isAddressBookCompatibleUseCase: IsAddressBookCompatibleUseCase,
    private val ownerLookupProducer: TxHistoryOwnerLookupProducer,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    fun isColdWallet(userWalletId: UserWalletId): Boolean {
        return userWalletsListRepository.getSyncOrNull(id = userWalletId) is UserWallet.Cold
    }

    /**
     * Creating a contact is offered for a foreign [recipient] this wallet has not saved yet — never for one of the
     * user's own addresses — and only while the address book can be written to: one stored by a newer build cannot.
     */
    fun canCreateContact(userWalletId: UserWalletId, recipient: String, network: Network): Flow<Boolean> = combine(
        flow = ownerLookupProducer(userWalletId),
        flow2 = isAddressBookCompatibleUseCase(userWalletId),
    ) { lookup, isCompatible ->
        isCompatible &&
            lookup.isSavableAsContact(ContactCandidate(address = recipient, networkId = network.id.rawId))
    }

    fun openContactCreation(address: String, network: Network) {
        router.push(
            AppRoute.AddressBook(
                AddressBookOpenMode.WithContactCreation(
                    address = address,
                    networkId = network.rawId,
                    source = AddressBookOpenMode.ContactCreationSource.TransactionDetails,
                ),
            ),
        )
    }

    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency, operationId: String): Flow<JointOperationState?> {
        if (currency.network.derivationPath !is Network.DerivationPath.Joint) return flowOf(null)

        return combine(
            flow = accountListSupplier.ownJointAccount(userWalletId = userWalletId, currency = currency),
            flow2 = jointAccountListSupplier(userWalletId = userWalletId),
        ) { account, accounts ->
            resolve(
                account = account,
                currency = currency,
                operationId = operationId,
                accounts = accounts,
            )
        }
            .distinctUntilChanged()
    }

    suspend fun sign(
        state: JointOperationState,
        network: Network,
        operationId: String,
    ): Either<JointOperationSignError, Unit> {
        return signJointOperationUseCase(
            account = state.account,
            network = network,
            operationId = operationId,
            displayed = state.displayed,
        )
    }

    suspend fun openSupport(userWalletId: UserWalletId) {
        val metaInfo = getWalletMetaInfoUseCase(userWalletId).getOrNull() ?: return

        sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(walletMetaInfo = metaInfo))
    }

    suspend fun reject(
        state: JointOperationState,
        network: Network,
        operationId: String,
    ): Either<JointOperationRejectError, Unit> {
        return rejectJointOperationUseCase(account = state.account, network = network, operationId = operationId)
    }

    private suspend fun resolve(
        account: Account.Joint?,
        currency: CryptoCurrency,
        operationId: String,
        accounts: List<JointAccount>,
    ): JointOperationState? {
        if (account == null) return null
        val jointAccount = accounts
            .firstOrNull { it.cryptoAccountId.equals(account.accountId.value, ignoreCase = true) }
            ?: return null

        val operation = jointAccount.inFlightOperations.values
            .firstOrNull { it.operationId == operationId }
            ?.takeIf { it.status != JointOperation.Status.UNKNOWN }
            ?: return null

        val safeAddress = jointAccount.safeAddress ?: return null
        val transfer = transferResolver.resolve(
            safeTx = operation.safeTx,
            currencies = account.cryptoCurrencies,
            network = currency.network,
            safeAddress = safeAddress,
        ) ?: return null

        val ownerAddress = ownerAddressResolver
            .resolve(userWalletId = account.accountId.userWalletId, ownerKeyIndex = account.ownerKeyIndex)
            .getOrNull()
        val signers = signersResolver.signers(jointAccount = jointAccount, operation = operation)

        return JointOperationState(
            account = account,
            displayed = transfer.intent,
            feeWei = operation.safeTx.gasPrice * (operation.safeTx.safeTxGas + operation.safeTx.baseGas),
            feeCoin = account.cryptoCurrencies
                .filterIsInstance<CryptoCurrency.Coin>()
                .firstOrNull { it.network == currency.network },
            threshold = jointAccount.threshold,
            membersCount = jointAccount.membersCount,
            signers = signers,
            canSign = signersResolver.canSign(
                jointAccount = jointAccount,
                operation = operation,
                ownerAddress = ownerAddress,
            ),
            ownAddress = ownerAddress,
            initiator = operation.initiator,
            isSending = operation.status == JointOperation.Status.PENDING,
        )
    }
}