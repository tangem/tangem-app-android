package com.tangem.tap.features.onboarding.products.wallet.ui

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.tangem.common.CardIdFormatter
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.ui.extensions.setStatusBarColor
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseMediator
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseRouter
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseViewModel
import com.tangem.sdk.ui.widget.leapfrogWidget.LeapfrogWidget
import com.tangem.sdk.ui.widget.leapfrogWidget.PropertyCalculator
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.BaseFragment
import com.tangem.tap.features.FragmentOnBackPressedHandler
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.onboarding.OnboardingMenuProvider
import com.tangem.tap.features.onboarding.products.wallet.redux.*
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.AccessCodeDialog
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.utils.Provider
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentOnboardingWalletBinding
import com.tangem.wallet.databinding.LayoutOnboardingSeedPhraseBinding
import com.tangem.wallet.databinding.ViewOnboardingProgressBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber

@Suppress("LargeClass", "MagicNumber")
@AndroidEntryPoint
class OnboardingWalletFragment :
    BaseFragment(R.layout.fragment_onboarding_wallet),
    StoreSubscriber<OnboardingWalletState>,
    FragmentOnBackPressedHandler {

    internal val binding: FragmentOnboardingWalletBinding by viewBinding(FragmentOnboardingWalletBinding::bind)
    internal val pbBinding: ViewOnboardingProgressBinding by viewBinding(ViewOnboardingProgressBinding::bind)

    internal val bindingSeedPhrase: LayoutOnboardingSeedPhraseBinding by lazy { binding.onboardingSeedPhraseContainer }

    private val canSkipBackup by lazy { arguments?.getBoolean(AppRoute.OnboardingWallet.CAN_SKIP_BACKUP_KEY) ?: true }

    private lateinit var seedPhraseStateHandler: OnboardingSeedPhraseStateHandler

    private val seedPhraseViewModel by viewModels<SeedPhraseViewModel>()

    private lateinit var cardsWidget: WalletCardsWidget
    private var seedPhraseRouter: SeedPhraseRouter? = null
    private var accessCodeDialog: AccessCodeDialog? = null

    private lateinit var animator: BackupAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedPhraseStateHandler = OnboardingSeedPhraseStateHandler(activity = requireActivity())

        val newSeedPhraseRouter = makeSeedPhraseRouter()
        seedPhraseRouter = newSeedPhraseRouter
        seedPhraseViewModel.setRouter(newSeedPhraseRouter)
        seedPhraseViewModel.setMediator(makeSeedPhraseMediator())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scaleFactor = getDeviceScaleFactor()
        initCardsWidget(createLeapfrogWidget(binding.flCardsContainer, scaleFactor), scaleFactor)

        binding.viewPagerBackupInfo.adapter = BackupInfoAdapter()
        TabLayoutMediator(
            binding.tabLayoutBackupInfo,
            binding.viewPagerBackupInfo,
        ) { tab, position ->
            // Some implementation
        }.attach()

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        addBackPressHandler(handler = this)

        store.dispatch(OnboardingWallet2Action.Init(seedPhraseViewModel.maxProgress))
        store.dispatch(OnboardingWalletAction.Init)
        store.dispatch(
            OnboardingWalletAction.LoadArtwork(
                cardArtworkUriForUnfinishedBackup = requireContext().resourceUri(R.drawable.card_placeholder_wallet),
            ),
        )
    }

    override fun loadToolbarMenu(): MenuProvider = OnboardingMenuProvider(
        scanResponseProvider = Provider {
            store.state.globalState.onboardingState.onboardingManager?.scanResponse
                ?: error("ScanResponse must be not null")
        },
    )

    private fun reInitCardsWidgetIfNeeded(backupCardsCounts: Int) = with(binding) {
        val viewBackupCount = flCardsContainer.childCount - 1
        if (viewBackupCount <= 0) return@with
        if (viewBackupCount == backupCardsCounts) return@with

        cardsWidget.toFolded(false)

        if (viewBackupCount > backupCardsCounts) {
            flCardsContainer.removeViews(0, viewBackupCount - backupCardsCounts)
        } else {
            flCardsContainer.inflate(R.layout.view_onboarding_card, true)
        }

        val scaleFactor = getDeviceScaleFactor()
        initCardsWidget(createLeapfrogWidget(flCardsContainer, scaleFactor), scaleFactor)
    }

    private fun createLeapfrogWidget(container: FrameLayout, deviceScaleFactor: Float): LeapfrogWidget {
        val leapfrogCalculator = PropertyCalculator(
            yTranslationFactor = when (container.childCount) {
                3 -> 25f
                else -> 35f
            } * deviceScaleFactor,
        )
        return LeapfrogWidget(container, leapfrogCalculator)
    }

    private fun initCardsWidget(leapfrogWidget: LeapfrogWidget, deviceScaleFactor: Float, isTest: Boolean = false) {
        cardsWidget = WalletCardsWidget(leapfrogWidget, deviceScaleFactor)
        animator = if (isTest) {
            TestBackupAnimation(WalletBackupAnimator(cardsWidget), binding)
        } else {
            WalletBackupAnimator(cardsWidget)
        }
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.onboardingWalletState == newState.onboardingWalletState
            }.select { it.onboardingWalletState }
        }
        setStatusBarColor(R.color.background_primary)
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: OnboardingWalletState) {
        if (activity == null || view == null) return

        animator.updateBackupState(state.backupState)
        requireActivity().invalidateOptionsMenu()

        pbBinding.pbState.max = state.getMaxProgress()
        pbBinding.pbState.progress = state.getProgressStep()

        when {
            state.wallet2State != null -> {
                seedPhraseStateHandler.newState(this, state, seedPhraseViewModel)
                state.cardArtworkUri?.let {
                    seedPhraseViewModel.setCardArtworkUri(it.toString())
                    if (state.isRingOnboarding) {
                        binding.imvFrontCard.load(R.drawable.img_ring_placeholder)
                    } else {
                        loadImageIntoImageView(state.cardArtworkUri, binding.imvFrontCard)
                    }
                    loadImageIntoImageView(it, binding.imvFirstBackupCard)
                    loadImageIntoImageView(it, binding.imvSecondBackupCard)
                }
            }
            else -> {
                if (state.isRingOnboarding) {
                    binding.imvFrontCard.load(R.drawable.img_ring_placeholder)
                } else {
                    loadImageIntoImageView(state.cardArtworkUri, binding.imvFrontCard)
                }
                loadImageIntoImageView(state.cardArtworkUri, binding.imvFirstBackupCard)
                loadImageIntoImageView(state.cardArtworkUri, binding.imvSecondBackupCard)
                handleOnboardingStep(state)
            }
        }
    }

    private fun loadImageIntoImageView(uri: Uri?, view: ImageView) {
        view.load(uri) {
            placeholder(R.drawable.card_placeholder_black)
            error(R.drawable.card_placeholder_black)
            fallback(R.drawable.card_placeholder_black)
        }
    }

    internal fun handleOnboardingStep(state: OnboardingWalletState) {
        when (state.step) {
            OnboardingWalletStep.CreateWallet -> setupCreateWalletState()
            OnboardingWalletStep.Backup -> setBackupState(
                state = state.backupState,
            )

            else -> {}
        }
    }

    private fun setupCreateWalletState() = with(binding) {
        layoutButtonsCommon.btnWalletMainAction.setText(R.string.onboarding_create_wallet_button_create_wallet)
        layoutButtonsCommon.btnWalletMainAction.setIconResource(R.drawable.ic_tangem_24)

        layoutButtonsCommon.btnWalletMainAction.setOnClickListener {
            Analytics.send(Onboarding.CreateWallet.ButtonCreateWallet())
            store.dispatch(OnboardingWalletAction.CreateWallet)
        }
        layoutButtonsCommon.btnWalletAlternativeAction.hide()

        toolbar.title = getText(R.string.onboarding_getting_started)

        tvHeader.setText(R.string.onboarding_create_wallet_header)
        tvBody.setText(R.string.onboarding_create_wallet_body)

        animator.setupCreateWalletState()
    }

    private fun setBackupState(state: BackupState) {
        when (state.backupStep) {
            BackupStep.InitBackup -> showBackupIntro(state)
            BackupStep.ScanOriginCard -> showScanOriginCard()
            BackupStep.AddBackupCards -> showAddBackupCards(state)
            BackupStep.SetAccessCode -> showSetAccessCode()
            BackupStep.EnterAccessCode -> showEnterAccessCode(state)
            BackupStep.ReenterAccessCode -> showReenterAccessCode(state)
            is BackupStep.WritePrimaryCard -> showWritePrimaryCard(state)
            is BackupStep.WriteBackupCard -> showWriteBackupCard(state)
            BackupStep.Finished -> showSuccess()
        }
    }

    private fun showBackupIntro(state: BackupState) = with(binding) {
        imvFirstBackupCard.show()
        imvSecondBackupCard.show()

        tvHeader.hide()
        tvBody.hide()
        viewPagerBackupInfo.show()
        tabLayoutBackupInfo.show()

        with(layoutButtonsCommon) {
            btnWalletMainAction.text = getText(R.string.onboarding_button_backup_now)
            btnWalletMainAction.icon = null
            btnWalletMainAction.setOnClickListener { store.dispatch(BackupAction.StartBackup) }

            btnWalletAlternativeAction.text = getText(R.string.onboarding_button_skip_backup)
            btnWalletAlternativeAction.setOnClickListener { store.dispatch(BackupAction.SkipBackup) }
            btnWalletAlternativeAction.show(state.canSkipBackup && canSkipBackup)
        }
        animator.showBackupIntro(state)
    }

    private fun showScanOriginCard() = with(binding) {
        prepareBackupView()
        tvHeader.text = getText(R.string.onboarding_title_scan_origin_card)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_primary,
        )

        with(layoutButtonsCommon) {
            btnWalletMainAction.text = getString(R.string.onboarding_button_scan_origin_card)
            btnWalletMainAction.setIconResource(R.drawable.ic_tangem_24)
            btnWalletAlternativeAction.hide()
            btnWalletMainAction.setOnClickListener { store.dispatch(BackupAction.ScanPrimaryCard) }
        }

        animator.showScanOriginCard()
    }

    private fun showAddBackupCards(state: BackupState) = with(binding) {
        prepareBackupView()

        accessCodeDialog?.dismiss()
        accessCodeDialog = null

        layoutButtonsAddCards.root.show()
        layoutButtonsCommon.root.hide()
        layoutButtonsAddCards.btnAddCard.setIconResource(R.drawable.ic_tangem_24)
        if (state.showBtnLoading) {
            layoutButtonsAddCards.btnAddCard.iconTint = ColorStateList.valueOf(
                resources.getColor(R.color.button_secondary),
            )
            layoutButtonsAddCards.btnAddCard.text = ""
            layoutButtonsAddCards.btnAddCard.isEnabled = false
            layoutButtonsAddCards.btnProgress.show()
        } else {
            layoutButtonsAddCards.btnAddCard.iconTint = ColorStateList.valueOf(
                resources.getColor(R.color.icon_primary_1),
            )
            layoutButtonsAddCards.btnProgress.hide()
            layoutButtonsAddCards.btnAddCard.text = getText(R.string.onboarding_button_add_backup_card)
            layoutButtonsAddCards.btnAddCard.isEnabled = true
        }
        if (state.backupCardsNumber < state.maxBackupCards) {
            layoutButtonsAddCards.btnAddCard.setOnClickListener {
                store.dispatch(BackupAction.AddBackupCard)
            }
        } else {
            layoutButtonsAddCards.btnAddCard.isEnabled = false
        }

        layoutButtonsAddCards.btnContinue.text = getText(R.string.onboarding_button_finalize_backup)
        layoutButtonsAddCards.btnContinue.setOnClickListener { store.dispatch(BackupAction.FinishAddingBackupCards) }
        layoutButtonsAddCards.btnContinue.isEnabled = state.backupCardsNumber != 0
        when (state.backupCardsNumber) {
            0 -> {
                tvHeader.text = getText(R.string.onboarding_title_no_backup_cards)
                tvBody.text = getText(R.string.onboarding_subtitle_no_backup_cards)
            }
            1 -> {
                tvHeader.text = getText(R.string.onboarding_title_one_backup_card)
                tvBody.text = getText(R.string.onboarding_subtitle_one_backup_card)
            }
            2 -> {
                tvHeader.text = getText(R.string.onboarding_title_two_backup_cards)
                tvBody.text = getText(R.string.onboarding_subtitle_two_backup_cards)
            }
            else -> {}
        }

        animator.showAddBackupCards(state, state.backupCardsNumber)
    }

    private fun prepareBackupView() = with(binding) {
        toolbar.title = getText(R.string.onboarding_navbar_title_creating_backup)

        tvHeader.show()
        tvBody.show()
        viewPagerBackupInfo.hide()
        tabLayoutBackupInfo.hide()

        imvFirstBackupCard.show()
        imvSecondBackupCard.show()
    }

    private fun showSetAccessCode() {
        accessCodeDialog = AccessCodeDialog(requireContext()).apply {
            dismissWithAnimation = true
            create()
            setOnCancelListener {
                store.dispatch(BackupAction.OnAccessCodeDialogClosed)
            }
            show()
            showInfoScreen()
            val view =
                delegate.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            (view?.parent as? ViewGroup)?.let { TransitionManager.beginDelayedTransition(it) }
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            view?.let { it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT }
        }
    }

    private fun showEnterAccessCode(state: BackupState) {
        accessCodeDialog?.showEnterAccessCode()
        accessCodeDialog?.showError(state.accessCodeError)
    }

    private fun showReenterAccessCode(state: BackupState) {
        accessCodeDialog?.showReenterAccessCode()
        accessCodeDialog?.showError(state.accessCodeError)
    }

    private fun showWritePrimaryCard(state: BackupState) = with(binding) {
        accessCodeDialog?.dismiss()

        reInitCardsWidgetIfNeeded(state.backupCardsNumber)
        prepareViewForFinalizeStep()

        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        tvHeader.text = getText(R.string.common_origin_card)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_primary_card_format,
            state.primaryCardId?.let { cardIdFormatter.getFormattedCardId(it) },
        )
        layoutButtonsCommon.btnWalletMainAction.text = getText(R.string.onboarding_button_backup_origin)
        layoutButtonsCommon.btnWalletMainAction.setIconResource(R.drawable.ic_tangem_24)
        layoutButtonsCommon.btnWalletMainAction.setOnClickListener { store.dispatch(BackupAction.WritePrimaryCard) }

        animator.showWritePrimaryCard(state)
    }

    private fun prepareViewForFinalizeStep() = with(binding) {
        toolbar.title = getText(R.string.onboarding_button_finalize_backup)

        layoutButtonsAddCards.root.hide()
        layoutButtonsCommon.root.show()
        layoutButtonsCommon.btnWalletAlternativeAction.hide()

        imvCardBackground.hide()
        imvFirstBackupCard.show()
        imvSecondBackupCard.show()
    }

    private fun showWriteBackupCard(state: BackupState) = with(binding) {
        reInitCardsWidgetIfNeeded(state.backupCardsNumber)
        prepareViewForFinalizeStep()

        val cardNumber = (state.backupStep as? BackupStep.WriteBackupCard)?.cardNumber ?: 1
        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        tvHeader.text = getString(R.string.onboarding_title_backup_card_format, cardNumber)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_backup_card_format,
            cardIdFormatter.getFormattedCardId(state.backupCardIds[cardNumber - 1]),
        )
        layoutButtonsCommon.btnWalletMainAction.text = getString(
            R.string.onboarding_button_backup_card_format,
            cardNumber,
        )
        layoutButtonsCommon.btnWalletMainAction.setIconResource(R.drawable.ic_tangem_24)
        layoutButtonsCommon.btnWalletMainAction.setOnClickListener {
            store.dispatch(BackupAction.WriteBackupCard(cardNumber))
        }

        animator.showWriteBackupCard(state, cardNumber)
    }

    private fun showSuccess() = with(binding) {
        toolbar.title = getString(R.string.onboarding_done_header)
        tvHeader.text = getText(R.string.onboarding_done_header)

        tvHeader.show()
        tvBody.show()
        viewPagerBackupInfo.hide()
        tabLayoutBackupInfo.hide()

        tvBody.text = getText(R.string.onboarding_subtitle_success_tangem_wallet_onboarding)
        layoutButtonsCommon.btnWalletMainAction.text = getText(R.string.onboarding_button_continue_wallet)
        layoutButtonsCommon.btnWalletMainAction.icon = null
        layoutButtonsCommon.btnWalletAlternativeAction.hide()
        layoutButtonsCommon.btnWalletMainAction.setOnClickListener {
            showConfetti(false)
            store.dispatch(OnboardingWalletAction.FinishOnboarding(scope = requireActivity().lifecycleScope))
        }

        animator.showSuccess {
            flCardsContainer.hide()
            imvCardBackground.hide()
            showConfetti(true)
            imvSuccess.alpha = 0f
            imvSuccess.show()
            imvSuccess.animate()?.alpha(1f)?.duration = 400
        }
    }

    internal fun showConfetti(show: Boolean) = with(binding.vConfetti) {
        lavConfetti.show(show)

        if (show) {
            lavConfetti.playAnimation()
        } else {
            lavConfetti.cancelAnimation()
        }
    }

    override fun handleOnBackPressed() {
        // workaround to use right navigation back for toolbar back btn on seed phrase flow
        val isWallet2 =
            store.state.globalState.onboardingState.onboardingManager?.scanResponse?.cardTypesResolver?.isWallet2()
                ?: false
        val seedPhraseRouter = seedPhraseRouter
        if (seedPhraseRouter != null && isWallet2) {
            seedPhraseRouter.navigateBack()
        } else {
            legacyOnBackHandler()
        }
    }

    private fun legacyOnBackHandler() {
        store.dispatch(OnboardingWalletAction.OnBackPressed)
    }

    private fun getDeviceScaleFactor(): Float {
        val typedValue = TypedValue()
        resources.getValue(R.dimen.device_scale_factor_for_twins_welcome, typedValue, true)
        return typedValue.float
    }

    private fun makeSeedPhraseRouter(): SeedPhraseRouter = SeedPhraseRouter(
        onBack = ::legacyOnBackHandler,
        onOpenChat = {
            Analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.Intro))
            // changed on email support [REDACTED_TASK_KEY]
            store.dispatch(
                GlobalAction.SendEmail(
                    feedbackData = SupportInfo(),
                    scanResponse = store.state.globalState.onboardingState.onboardingManager?.scanResponse
                        ?: error("ScanResponse must be not null"),
                ),
            )
        },
        onOpenUriClick = { uri ->
            store.dispatchOpenUrl(uri.toString())
        },
    )

    private fun makeSeedPhraseMediator(): SeedPhraseMediator {
        return object : SeedPhraseMediator {
            override fun createWallet(callback: (CompletionResult<CreateWalletResponse>) -> Unit) {
                store.dispatch(OnboardingWallet2Action.CreateWallet(callback))
            }

            override fun onWalletCreated(result: CompletionResult<CreateWalletResponse>) {
                store.dispatch(OnboardingWallet2Action.WalletWasCreated(result))
            }

            override fun importWallet(
                mnemonicComponents: List<String>,
                passphrase: String?,
                seedPhraseSource: SeedPhraseSource,
                callback: (CompletionResult<CreateWalletResponse>) -> Unit,
            ) {
                store.dispatch(
                    OnboardingWallet2Action.ImportWallet(
                        mnemonicComponents,
                        passphrase,
                        seedPhraseSource,
                        callback,
                    ),
                )
            }

            override fun allowScreenshots(allow: Boolean) {
                mainScope.launch {
                    if (allow) {
                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        requireActivity().window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    }
                }
            }
        }
    }
}