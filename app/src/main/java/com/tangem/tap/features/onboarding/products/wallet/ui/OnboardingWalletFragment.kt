package com.tangem.tap.features.onboarding.products.wallet.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.tangem.common.CardIdFormatter
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.leapfrogWidget.LeapfrogWidget
import com.tangem.tap.common.postUi
import com.tangem.tap.features.FragmentOnBackPressedHandler
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.home.redux.HomeMiddleware
import com.tangem.tap.features.onboarding.products.wallet.redux.*
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.AccessCodeDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding_wallet.*
import kotlinx.android.synthetic.main.layout_onboarding_buttons_add_cards.*
import kotlinx.android.synthetic.main.layout_onboarding_buttons_common.*
import kotlinx.android.synthetic.main.view_confetti.*
import kotlinx.android.synthetic.main.view_onboarding_progress.*
import org.rekotlin.StoreSubscriber

class OnboardingWalletFragment : Fragment(R.layout.fragment_onboarding_wallet),
    StoreSubscriber<OnboardingWalletState>, FragmentOnBackPressedHandler {

    private var accessCodeDialog: AccessCodeDialog? = null
    private lateinit var cardsWidget: BackupCardsWidget

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

        val leapfrog = LeapfrogWidget(fl_cards_container)
        cardsWidget = BackupCardsWidget(leapfrog) { 200f }
        startPostponedEnterTransition()

        view_pager_backup_info.adapter = BackupInfoAdapter()
        TabLayoutMediator(tab_layout_backup_info, view_pager_backup_info) { tab, position ->
            //Some implementation
        }.attach()

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        store.dispatch(OnboardingWalletAction.Init)
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

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
        if (activity == null) return

        requireActivity().invalidateOptionsMenu()

        state.artwork?.artwork?.let { imv_front_card.setImageBitmap(it) }
        pb_state.max = 6
        pb_state.progress = state.getProgressStep()

        when (state.step) {
            OnboardingWalletStep.None -> {
            }
            OnboardingWalletStep.CreateWallet -> setupCreateWalletState()
            OnboardingWalletStep.Backup -> setBackupState(state.backupState)
            OnboardingWalletStep.Done -> {

            }
        }
    }

    private fun setupCreateWalletState() {
        btn_main_action.setText(R.string.onboarding_create_wallet_button_create_wallet)
        btn_main_action.setOnClickListener { store.dispatch(OnboardingWalletAction.CreateWallet) }
        btn_alternative_action.hide()

        toolbar.title = getText(R.string.onboarding_getting_started)

        tv_header.setText(R.string.onboarding_create_wallet_header)
        tv_body.setText(R.string.onboarding_create_wallet_body)

        cardsWidget.toFolded()
        startPostponedEnterTransition()
    }


    private fun setBackupState(state: BackupState) {
        when (state.backupStep) {
            BackupStep.InitBackup -> showBackupIntro(state)
            BackupStep.ScanOriginCard -> showScanOriginCard(state)
            BackupStep.AddBackupCards -> showAddBackupCards(state)
            BackupStep.SetAccessCode -> showSetAccessCode()
            BackupStep.EnterAccessCode -> showEnterAccessCode(state)
            BackupStep.ReenterAccessCode -> showReenterAccessCode(state)
            is BackupStep.WritePrimaryCard -> showWritePrimaryCard(state)
            is BackupStep.WriteBackupCard -> showWriteBackupCard(state)
            BackupStep.Finished -> showSuccess(state)
        }
    }

    private fun showBackupIntro(state: BackupState) {
        imv_first_backup_card.show()
        imv_second_backup_card.show()

        cardsWidget.toWelcome()

        tv_header.hide()
        tv_body.hide()
        view_pager_backup_info.show()
        tab_layout_backup_info.show()

        btn_main_action.text = getText(R.string.onboarding_button_backup_now)
        btn_alternative_action.text = getText(R.string.onboarding_button_skip_backup)
        btn_alternative_action.show(state.canSkipBackup)
        btn_main_action.setOnClickListener { store.dispatch(BackupAction.StartBackup) }
        btn_alternative_action.setOnClickListener { store.dispatch(BackupAction.DismissBackup) }
        startPostponedEnterTransition()
    }

    private fun showScanOriginCard(state: BackupState) {

        toolbar.title = getText(R.string.onboarding_navbar_title_creating_backup)

        cardsWidget.toFolded()

        tv_header.show()
        tv_body.show()
        view_pager_backup_info.hide()
        tab_layout_backup_info.hide()

        imv_first_backup_card.show()
        imv_second_backup_card.show()

        tv_header.text = getText(R.string.onboarding_title_scan_origin_card)
        tv_body.text = getString(
            R.string.onboarding_subtitle_scan_origin_card,
        )

        btn_main_action.text = getString(R.string.onboarding_button_scan_origin_card)
        btn_alternative_action.hide()
        btn_main_action.setOnClickListener { store.dispatch(BackupAction.ScanPrimaryCard) }
    }

    private fun showAddBackupCards(state: BackupState) {
        imv_first_backup_card.show()
        imv_second_backup_card.show()

        accessCodeDialog?.dismiss()
        accessCodeDialog = null

        layout_buttons_add_cards.show()
        layout_buttons_common.hide()
        if (state.backupCardsNumber < state.maxBackupCards) {
            btn_add_card.text = getText(R.string.onboarding_button_add_backup_card)
            btn_add_card.setOnClickListener { store.dispatch(BackupAction.AddBackupCard) }
        } else {
            btn_add_card.text = "You hit the maximum"
            btn_add_card.isEnabled = false
        }

        when (state.backupCardsNumber) {
            0 -> {
                cardsWidget.toFan()
                tv_header.text = getText(R.string.onboarding_title_no_backup_cards)
                tv_body.text = getText(R.string.onboarding_subtitle_no_backup_cards)
                cardsWidget.getFirstBackupCardView().alpha = 0.6f
                cardsWidget.getSecondBackupCardView().alpha = 0.2f
            }
            1 -> {
                tv_header.text = getText(R.string.onboarding_title_one_backup_card)
                tv_body.text = getText(R.string.onboarding_subtitle_one_backup_card)

                cardsWidget.getFirstBackupCardView().animate().alpha(1f).setDuration(400)
                cardsWidget.getSecondBackupCardView().alpha = 0.2f
            }
            2 -> {
                tv_header.text = getText(R.string.onboarding_title_two_backup_cards)
                tv_body.text = getText(R.string.onboarding_subtitle_two_backup_cards)
                cardsWidget.getFirstBackupCardView().alpha = 1f
                cardsWidget.getSecondBackupCardView().animate().alpha(1f).setDuration(400)
            }
        }

        btn_continue.text = getText(R.string.onboarding_button_finalize_backup)
        btn_continue.setOnClickListener { store.dispatch(BackupAction.FinishAddingBackupCards) }
        btn_continue.isEnabled = state.backupCardsNumber != 0
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
        postUi(2000) { store.dispatch(BackupAction.SetAccessCodeError(null)) }

    }

    private fun showReenterAccessCode(state: BackupState) {
        accessCodeDialog?.showReenterAccessCode()
        accessCodeDialog?.showError(state.accessCodeError)
        postUi(2000) { store.dispatch(BackupAction.SetAccessCodeError(null)) }

    }

    private fun showWritePrimaryCard(state: BackupState) {
        accessCodeDialog?.dismiss()

        prepareViewForFinalizeStep()
        cardsWidget.getSecondBackupCardView().show(state.backupCardsNumber == 2)

        cardsWidget.toLeapfrog() {
            cardsWidget.getFirstBackupCardView().alpha = 0.6f
            cardsWidget.getSecondBackupCardView().alpha = 0.2f
        }

        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        tv_header.text = getText(R.string.onboarding_title_prepare_origin)
        tv_body.text = getString(
            R.string.onboarding_subtitle_scan_primary_card_format,
            state.primaryCardId?.let { cardIdFormatter.getFormattedCardId(it) }
        )
        btn_main_action.text = getText(R.string.onboarding_button_backup_origin)
        btn_main_action.setOnClickListener { store.dispatch(BackupAction.WritePrimaryCard) }

    }

    private fun prepareViewForFinalizeStep() {
        layout_buttons_add_cards.hide()
        layout_buttons_common.show()

        imv_first_backup_card.show()
        imv_second_backup_card.show()

        toolbar.title = getText(R.string.onboarding_button_finalize_backup)

        imv_card_background.hide()

        cardsWidget.toLeapfrog()

//        imv_front_card.alpha = 1f
//        imv_first_backup_card.alpha = 1f
//        imv_second_backup_card.alpha = 1f

        btn_alternative_action.hide()
    }

    private fun showWriteBackupCard(state: BackupState) {
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
        tv_header.text =
            getString(R.string.onboarding_title_backup_card_format, cardNumber)
        tv_body.text = getString(
            R.string.onboarding_subtitle_scan_primary_card_format,
            cardIdFormatter.getFormattedCardId(state.backupCardIds[cardNumber - 1])
        )
        btn_main_action.text = getString(
            R.string.onboarding_button_backup_card_format,
            cardNumber
        )

        btn_main_action.setOnClickListener { store.dispatch(BackupAction.WriteBackupCard(cardNumber)) }
    }

    private fun showSuccess(state: BackupState) {
        tv_header.text = getText(R.string.onboarding_done_header)
        val text = when (state.backupCardsNumber) {
            1 -> getText(R.string.onboarding_subtitle_success_backup_one_card)
            2 -> getText(R.string.onboarding_subtitle_success_backup)
            else -> getText(R.string.onboarding_subtitle_success_tangem_wallet_onboarding)
        }

        tv_header.show()
        tv_body.show()
        view_pager_backup_info.hide()
        tab_layout_backup_info.hide()

        tv_body.text = text
        btn_main_action.text = getText(R.string.common_continue)
        btn_alternative_action.hide()
        btn_main_action.setOnClickListener {
            lav_confetti.cancelAnimation()
            lav_confetti.hide()
            store.dispatch(OnboardingWalletAction.FinishOnboarding)
        }

        cardsWidget.leapfrogWidget.fold {
            fl_cards_container?.hide()
            imv_card_background?.hide()
            lav_confetti?.show()
            lav_confetti?.playAnimation()
            imv_success?.alpha = 0f
            imv_success?.show()

            imv_success?.animate()
                ?.alpha(1f)
                ?.setDuration(400)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.shop_menu -> {
                store.dispatchOpenUrl(HomeMiddleware.CARD_SHOP_URI)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.shop, menu)

        val backupStep = store.state.onboardingWalletState.backupState.backupStep
        val shopMenuShouldBeVisible =
            backupStep == BackupStep.ScanOriginCard || backupStep == BackupStep.AddBackupCards
        menu.getItem(0).isVisible = shopMenuShouldBeVisible
    }

    override fun handleOnBackPressed() {
        store.dispatch(OnboardingWalletAction.OnBackPressed)
    }
}