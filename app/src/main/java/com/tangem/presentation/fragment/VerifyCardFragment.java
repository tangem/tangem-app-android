package com.tangem.presentation.fragment;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.wallet.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class VerifyCardFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, NfcAdapter.ReaderCallback {

    TangemCard mCard;
    TextView tvCardID, tvManufacturer, tvRegistrationDate, tvCardIdentity, tvLastSigned, tvRemainingSignatures, tvReusable, tvOk, tvError, tvMessage,
            tvIssuer, tvIssuerData, tvFeatures, tvBlockchain, tvSignedTx, tvSigningMethod, tvFirmware, tvWalletIdentity, tvWallet;
    ImageView ivBlockchain, ivPIN, ivPIN2orSecurityDelay, ivDeveloperVersion;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private NfcManager mNfcManager;

    public VerifyCardFragment() {

    }

    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fr_verify_card, container, false);

        mNfcManager = new NfcManager(this.getActivity(), this);


        // SwipeRefreshLayout
        mSwipeRefreshLayout = v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mCard = new TangemCard(getActivity().getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getActivity().getIntent().getExtras().getBundle("Card"));
        tvCardID = v.findViewById(R.id.tvCardID);

        tvLastSigned = v.findViewById(R.id.tvLastSigned);
        tvRemainingSignatures = v.findViewById(R.id.tvRemainingSignatures);

        tvReusable = v.findViewById(R.id.tvReusable);

        tvManufacturer = v.findViewById(R.id.tvManufacturerInfo);

        tvCardIdentity = v.findViewById(R.id.tvCardIdentity);

        tvRegistrationDate = v.findViewById(R.id.tvCardRegistredDate);

        ivBlockchain = v.findViewById(R.id.imgBlockchain);
        ivPIN = v.findViewById(R.id.imgPIN);
        ivPIN2orSecurityDelay = v.findViewById(R.id.imgPIN2orSecurityDelay);
        ivDeveloperVersion = v.findViewById(R.id.imgDeveloperVersion);

        tvError = v.findViewById(R.id.tvError);
        tvMessage = v.findViewById(R.id.tvMessage);

        tvIssuer = v.findViewById(R.id.tvIssuer);
        tvIssuerData = v.findViewById(R.id.tvIssuerData);

        tvFirmware = v.findViewById(R.id.tvFirmware);
        tvFeatures = v.findViewById(R.id.tvFeatures);
        tvBlockchain = v.findViewById(R.id.tvBlockchain);

        tvSignedTx = v.findViewById(R.id.tvSignedTx);
        tvSigningMethod = v.findViewById(R.id.tvSigningMethod);

        tvOk = v.findViewById(R.id.tvOk);
        if (tvOk != null) {
            tvOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent data = prepareResultIntent();
                    data.putExtra("modification", "update");
                    getActivity().setResult(Activity.RESULT_OK, data);
                    getActivity().finish();
                }
            });
        }

        tvWallet = v.findViewById(R.id.tvWallet);
        tvWalletIdentity = v.findViewById(R.id.tvWalletIdentity);

        UpdateViews();

//        if (NeedUpdate) {
//            mSwipeRefreshLayout.setRefreshing(true);
//            mSwipeRefreshLayout.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    onRefresh();
//                }
//            }, 1000);
//        }
        return v;
    }

    void UpdateViews() {
        try {
            if (timerHideErrorAndMessage != null) {
                timerHideErrorAndMessage.cancel();
                timerHideErrorAndMessage = null;
            }
            tvCardID.setText(mCard.getCIDDescription());

            if (mCard.getError() == null || mCard.getError().isEmpty()) {
                tvError.setVisibility(View.GONE);
                tvError.setText("");
            } else {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(mCard.getError());
            }
            if (mCard.getMessage() == null || mCard.getMessage().isEmpty()) {
                tvMessage.setVisibility(View.GONE);
                tvMessage.setText("");
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(mCard.getMessage());
            }

            tvManufacturer.setText(mCard.getManufacturer().getOfficialName());

            if (mCard.isManufacturerConfirmed() && mCard.isCardPublicKeyValid()) {
                tvCardIdentity.setText("Attested");
                tvCardIdentity.setTextColor(getResources().getColor(R.color.confirmed, getActivity().getTheme()));
            } else {
                tvCardIdentity.setText("Not confirmed");
                tvCardIdentity.setTextColor(getResources().getColor(R.color.not_confirmed, getActivity().getTheme()));
            }

            tvIssuer.setText(mCard.getIssuerDescription());
            tvIssuerData.setText(mCard.getIssuerDataDescription());

            tvRegistrationDate.setText(mCard.getPersonalizationDateTimeDescription());

            //tvBlockchain.setText(mCard.getBlockchain().getOfficialName());
            tvBlockchain.setText(mCard.getBlockchainName());
            ivBlockchain.setImageResource(mCard.getBlockchain().getImageResource(this.getContext(), mCard.getTokenSymbol()));

            if (mCard.isReusable()) {
                tvReusable.setText("Reusable");
            } else {
                tvReusable.setText("One-off banknote");
            }

            tvSigningMethod.setText(mCard.getSigningMethod().getDescription());

            if (mCard.getStatus() == TangemCard.Status.Loaded || mCard.getStatus() == TangemCard.Status.Purged) {

                tvLastSigned.setText(mCard.getLastSignedDescription());
                if (mCard.getRemainingSignatures() == 0) {
                    tvRemainingSignatures.setTextColor(getResources().getColor(R.color.not_confirmed, getActivity().getTheme()));
                    tvRemainingSignatures.setText("None");
                } else if (mCard.getRemainingSignatures() == 1) {
                    tvRemainingSignatures.setTextColor(getResources().getColor(R.color.not_confirmed, getActivity().getTheme()));
                    tvRemainingSignatures.setText("Last one!");
                } else if (mCard.getRemainingSignatures() > 1000) {
                    tvRemainingSignatures.setTextColor(getResources().getColor(R.color.confirmed, getActivity().getTheme()));
                    tvRemainingSignatures.setText("Unlimited");
                } else {
                    tvRemainingSignatures.setTextColor(getResources().getColor(R.color.confirmed, getActivity().getTheme()));
                    tvRemainingSignatures.setText(String.valueOf(mCard.getRemainingSignatures()));
                }
                tvSignedTx.setText(String.valueOf(mCard.getMaxSignatures() - mCard.getRemainingSignatures()));
            } else {
                tvLastSigned.setText("");
                tvRemainingSignatures.setText("");
                tvSignedTx.setText("");
            }

            tvFirmware.setText(mCard.getFirmwareVersion());

            String features = "";

            if (mCard.allowSwapPIN() && mCard.allowSwapPIN2()) {
                features += "Allows change PIN1 and PIN2\n";
            } else if (mCard.allowSwapPIN()) {
                features += "Allows change PIN1\n";
            } else if (mCard.allowSwapPIN2()) {
                features += "Allows change PIN2\n";
            } else {
                features += "Fixed PIN1 and PIN2\n";
            }

            if (mCard.needCVC()) {
                features += "Requires CVC\n";
            }

            if (mCard.supportDynamicNDEF()) {
                features += "Dynamic NDEF for iOS\n";
            } else if (mCard.supportNDEF()) {
                features += "NDEF\n";
            }

            if (mCard.supportBlock()) {
                features += "Blockable\n";
            }

            if (mCard.supportOnlyOneCommandAtTime()) {
                features += "Atomic command mode";
            }

            if (features.endsWith("\n")) {
                features = features.substring(0, features.length() - 1);
            }
            tvFeatures.setText(features);

            if (mCard.useDefaultPIN1()) {
                ivPIN.setImageResource(R.drawable.unlock_pin1);
                ivPIN.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by default PIN1 code", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                ivPIN.setImageResource(R.drawable.lock_pin1);
                ivPIN.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by user's PIN1 code", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (mCard.getPauseBeforePIN2() > 0 && (mCard.useDefaultPIN2() || !mCard.useSmartSecurityDelay())) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.timer);
                ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), String.format("This banknote will enforce %.0f seconds security delay for all operations requiring PIN2 code", mCard.getPauseBeforePIN2() / 1000.0), Toast.LENGTH_LONG).show();
                    }
                });

            } else if (mCard.useDefaultPIN2()) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by default PIN2 code", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by user's PIN2 code", Toast.LENGTH_LONG).show();
                    }
                });
            }


            if (mCard.useDevelopersFirmware()) {
                ivDeveloperVersion.setImageResource(R.drawable.ic_developer_version);
                ivDeveloperVersion.setVisibility(View.VISIBLE);
                ivDeveloperVersion.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "Unlocked banknote, only for development use", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                ivDeveloperVersion.setVisibility(View.INVISIBLE);
            }

            if (mCard.getStatus() == TangemCard.Status.Loaded) {
                tvWallet.setText(mCard.getShortWalletString());
                if (mCard.isWalletPublicKeyValid()) {
                    tvWalletIdentity.setText("Possession proved");
                    tvWalletIdentity.setTextColor(getResources().getColor(R.color.confirmed, getActivity().getTheme()));
                } else {
                    tvWalletIdentity.setText("Possession NOT proved");
                    tvWalletIdentity.setTextColor(getResources().getColor(R.color.not_confirmed, getActivity().getTheme()));
                }
            } else {
                tvWallet.setText("not available");
                tvWalletIdentity.setText("-- -- --");
            }

            timerHideErrorAndMessage = new Timer();
            timerHideErrorAndMessage.schedule(new TimerTask() {
                @Override
                public void run() {
                    tvError.post(new Runnable() {
                        @Override
                        public void run() {
                            tvMessage.setVisibility(View.GONE);
                            tvError.setVisibility(View.GONE);
                            mCard.setError(null);
                            mCard.setMessage(null);
                        }
                    });
                }
            }, 5000);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Timer timerHideErrorAndMessage = null;

    public Intent prepareResultIntent() {
        Intent data = new Intent();
        data.putExtra("UID", mCard.getUID());
        data.putExtra("Card", mCard.getAsBundle());
        return data;
    }


    @Override
    public void onResume() {
        super.onResume();
        mNfcManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfcManager.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mNfcManager.onStop();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            Log.w(getClass().getName(), "Ignore discovered tag!");
            mNfcManager.IgnoreTag(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
