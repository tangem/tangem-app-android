package com.tangem.tap.features.onboarding.products.wallet.ui

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import com.tangem.common.CardIdFormatter
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.FragmentOnBackPressedHandler
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.onboarding.products.wallet.redux.*
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.AccessCodeDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentOnboardingWalletBinding
import com.tangem.wallet.databinding.ViewOnboardingProgressBinding
import org.rekotlin.StoreSubscriber

class OnboardingWalletFragment : Fragment(R.layout.fragment_onboarding_wallet),
    StoreSubscriber<OnboardingWalletState>, FragmentOnBackPressedHandler {

    private var accessCodeDialog: AccessCodeDialog? = null
    private lateinit var cardsWidget: WalletCardsWidget
    private val binding: FragmentOnboardingWalletBinding by viewBinding(
        FragmentOnboardingWalletBinding::bind
    )
    private val pbBinding: ViewOnboardingProgressBinding by viewBinding(
        ViewOnboardingProgressBinding::bind)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        setHasOptionsMenu(true)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val leapfrog = LeapfrogWidget(binding.flCardsContainer)
        cardsWidget = WalletCardsWidget(leapfrog) { 200f }
        startPostponedEnterTransition()

        binding.viewPagerBackupInfo.adapter = BackupInfoAdapter()
        TabLayoutMediator(
            binding.tabLayoutBackupInfo,
            binding.viewPagerBackupInfo
        ) { tab, position ->
            //Some implementation
        }.attach()

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        store.dispatch(OnboardingWalletAction.Init)
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        store.dispatch(OnboardingWalletAction.LoadArtwork)

        addBackPressHandler(this)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.onboardingWalletState == newState.onboardingWalletState
            }.select { it.onboardingWalletState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: OnboardingWalletState) {
        if (activity == null || view == null) return

        requireActivity().invalidateOptionsMenu()

        with(binding) {
            loadImageIntoImageView(state.cardArtworkUrl, imvFrontCard)
            loadImageIntoImageView(state.cardArtworkUrl, imvFirstBackupCard)
            loadImageIntoImageView(state.cardArtworkUrl, imvSecondBackupCard)
        }

        pbBinding.pbState.max = 6
        pbBinding.pbState.progress = state.getProgressStep()

        when (state.step) {
            OnboardingWalletStep.None -> {
            }
            OnboardingWalletStep.CreateWallet -> setupCreateWalletState()
            OnboardingWalletStep.Backup -> setBackupState(state.backupState)
            OnboardingWalletStep.Done -> {

            }
        }
    }

    private fun loadImageIntoImageView(url: String?, view: ImageView) {
        Picasso.get()
            .load(url)
            .error(R.drawable.card_placeholder_black)
            .placeholder(R.drawable.card_placeholder_black)
            ?.into(view)
    }

    private fun setupCreateWalletState() = with(binding) {
        layoutButtonsCommon.btnMainAction.setText(R.string.onboarding_create_wallet_button_create_wallet)
        layoutButtonsCommon.btnMainAction.setOnClickListener { store.dispatch(OnboardingWalletAction.CreateWallet) }
        layoutButtonsCommon.btnAlternativeAction.hide()

        toolbar.title = getText(R.string.onboarding_getting_started)

        tvHeader.setText(R.string.onboarding_create_wallet_header)
        tvBody.setText(R.string.onboarding_create_wallet_body)

        cardsWidget.toFolded()
        startPostponedEnterTransition()
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

        cardsWidget.toWelcome()

        tvHeader.hide()
        tvBody.hide()
        viewPagerBackupInfo.show()
        tabLayoutBackupInfo.show()

        with(layoutButtonsCommon) {
            btnMainAction.text = getText(R.string.onboarding_button_backup_now)
            btnAlternativeAction.text = getText(R.string.onboarding_button_skip_backup)
            btnAlternativeAction.show(state.canSkipBackup)
            btnMainAction.setOnClickListener { store.dispatch(BackupAction.StartBackup) }
            btnAlternativeAction.setOnClickListener { store.dispatch(BackupAction.DismissBackup) }
        }

        startPostponedEnterTransition()
    }

    private fun showScanOriginCard() = with(binding) {
        prepareBackupView()

        cardsWidget.toFolded()

        tvHeader.text = getText(R.string.onboarding_title_scan_origin_card)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_origin_card,
        )

        with(layoutButtonsCommon) {
            btnMainAction.text = getString(R.string.onboarding_button_scan_origin_card)
            btnAlternativeAction.hide()
            btnMainAction.setOnClickListener { store.dispatch(BackupAction.ScanPrimaryCard) }
        }
    }

    private fun prepareBackupView() = with(binding) {
        toolbar.title = getText(R.string.onboarding_navbar_title_creating_backup)

        tvHeader.show()
        tvBody.show()
        viewPagerBackupInfo.hide()
        tabLayoutBackupInfo.hide()

        imvSecondBackupCard.show()
        imvSecondBackupCard.show()
    }

    private fun showAddBackupCards(state: BackupState) = with(binding) {
        prepareBackupView()

        accessCodeDialog?.dismiss()
        accessCodeDialog = null

        layoutButtonsAddCards.root.show()
        layoutButtonsCommon.root.hide()
        layoutButtonsAddCards.btnAddCard.text = getText(R.string.onboarding_button_add_backup_card)
        if (state.backupCardsNumber < state.maxBackupCards) {
            layoutButtonsAddCards.btnAddCard.setOnClickListener { store.dispatch(BackupAction.AddBackupCard) }
        } else {
            layoutButtonsAddCards.btnAddCard.isEnabled = false
        }

        when (state.backupCardsNumber) {
            0 -> {
                cardsWidget.toFan() {
                    cardsWidget.getFirstBackupCardView().animate().alpha(0.6f).setDuration(200)
                    cardsWidget.getSecondBackupCardView().animate().alpha(0.2f).setDuration(200)
                }
                tvHeader.text = getText(R.string.onboarding_title_no_backup_cards)
                tvBody.text = getText(R.string.onboarding_subtitle_no_backup_cards)
            }
            1 -> {
                tvHeader.text = getText(R.string.onboarding_title_one_backup_card)
                tvBody.text = getText(R.string.onboarding_subtitle_one_backup_card)

                cardsWidget.getFirstBackupCardView().animate().alpha(1f).setDuration(400)
                cardsWidget.getSecondBackupCardView().alpha = 0.2f
            }
            2 -> {
                tvHeader.text = getText(R.string.onboarding_title_two_backup_cards)
                tvBody.text = getText(R.string.onboarding_subtitle_two_backup_cards)
                cardsWidget.getFirstBackupCardView().alpha = 1f
                cardsWidget.getSecondBackupCardView().animate().alpha(1f).setDuration(400)
            }
        }

        layoutButtonsAddCards.btnContinue.text = getText(R.string.onboarding_button_finalize_backup)
        layoutButtonsAddCards.btnContinue.setOnClickListener { store.dispatch(BackupAction.FinishAddingBackupCards) }
        layoutButtonsAddCards.btnContinue.isEnabled = state.backupCardsNumber != 0
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

        prepareViewForFinalizeStep()
        cardsWidget.getSecondBackupCardView().show(state.backupCardsNumber == 2)

        cardsWidget.toLeapfrog() {
            cardsWidget.getFirstBackupCardView().alpha = 0.6f
            cardsWidget.getSecondBackupCardView().alpha = 0.2f
        }

        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        tvHeader.text = getText(R.string.onboarding_title_prepare_origin)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_primary_card_format,
            state.primaryCardId?.let { cardIdFormatter.getFormattedCardId(it) }
        )
        layoutButtonsCommon.btnMainAction.text = getText(R.string.onboarding_button_backup_origin)
        layoutButtonsCommon.btnMainAction.setOnClickListener { store.dispatch(BackupAction.WritePrimaryCard) }

    }

    private fun prepareViewForFinalizeStep() = with(binding) {
        layoutButtonsAddCards.root.hide()
        layoutButtonsCommon.root.show()

        imvFirstBackupCard.show()
        imvSecondBackupCard.show()

        toolbar.title = getText(R.string.onboarding_button_finalize_backup)

        imvCardBackground.hide()

        cardsWidget.toLeapfrog()

        layoutButtonsCommon.btnAlternativeAction.hide()
    }

    private fun showWriteBackupCard(state: BackupState) = with(binding) {
        prepareViewForFinalizeStep()

        cardsWidget.getSecondBackupCardView().show(state.backupCardsNumber == 2)

        val cardNumber = (state.backupStep as? BackupStep.WriteBackupCard)?.cardNumber ?: 1
        when (cardNumber) {
            1 -> cardsWidget.leapfrogWidget.leap() {
                cardsWidget.getOriginCardView().alpha = 0.4f
                cardsWidget.getFirstBackupCardView().alpha = 0.2f
                cardsWidget.getSecondBackupCardView().alpha = 1f
            }
            2 -> cardsWidget.leapfrogWidget.leap() {
                cardsWidget.getOriginCardView().alpha = 0.4f
                cardsWidget.getFirstBackupCardView().alpha = 0.2f
                cardsWidget.getSecondBackupCardView().alpha = 1f
            }
        }

        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        tvHeader.text =
            getString(R.string.onboarding_title_backup_card_format, cardNumber)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_backup_card_format,
            cardIdFormatter.getFormattedCardId(state.backupCardIds[cardNumber - 1])
        )
        layoutButtonsCommon.btnMainAction.text = getString(
            R.string.onboarding_button_backup_card_format,
            cardNumber
        )
        layoutButtonsCommon.btnMainAction.setOnClickListener {
            store.dispatch(BackupAction.WriteBackupCard(cardNumber))
        }
    }

    private fun showSuccess() = with(binding) {
        tvHeader.text = getText(R.string.onboarding_done_header)

        tvHeader.show()
        tvBody.show()
        viewPagerBackupInfo.hide()
        tabLayoutBackupInfo.hide()

        tvBody.text = getText(R.string.onboarding_subtitle_success_tangem_wallet_onboarding)
        layoutButtonsCommon.btnMainAction.text = getText(R.string.onboarding_button_continue_wallet)
        layoutButtonsCommon.btnAlternativeAction.hide()
        layoutButtonsCommon.btnMainAction.setOnClickListener {
            vConfetti.lavConfetti.cancelAnimation()
            vConfetti.lavConfetti.hide()
            store.dispatch(OnboardingWalletAction.FinishOnboarding)
        }

        cardsWidget.leapfrogWidget.fold {
            flCardsContainer.hide()
            imvCardBackground.hide()
            vConfetti.lavConfetti.show()
            vConfetti.lavConfetti.playAnimation()
            imvSuccess.alpha = 0f
            imvSuccess.show()

            imvSuccess.animate()
                ?.alpha(1f)
                ?.setDuration(400)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.shop_menu -> {
                store.dispatch(BackupAction.GoToShop)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.shop, menu)

        val backupState = store.state.onboardingWalletState.backupState
        val backupStep = backupState.backupStep

        val shopMenuShouldBeVisible =
            (backupStep == BackupStep.ScanOriginCard || backupStep == BackupStep.AddBackupCards) &&
                    backupState.buyAdditionalCardsUrl != null
        menu.getItem(0).isVisible = shopMenuShouldBeVisible
    }

    override fun handleOnBackPressed() {
        store.dispatch(OnboardingWalletAction.OnBackPressed)
    }
}