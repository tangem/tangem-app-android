package com.tangem.di;

import com.tangem.presentation.activity.EmptyWalletActivity;
import com.tangem.presentation.activity.LoadedWalletActivity;
import com.tangem.presentation.activity.LogoActivity;
import com.tangem.presentation.activity.MainActivity;
import com.tangem.presentation.activity.PrepareCryptonitOtherApiWithdrawalActivity;
import com.tangem.presentation.activity.PrepareKrakenWithdrawalActivity;
import com.tangem.presentation.activity.PrepareTransactionActivity;
import com.tangem.presentation.activity.VerifyCardActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
//        AppModule.class,
        NavigatorModule.class})
public interface NavigatorComponent {

    void inject(LogoActivity activity);

    void inject(MainActivity activity);

    void inject(PrepareTransactionActivity activity);

    void inject(PrepareCryptonitOtherApiWithdrawalActivity activity);

    void inject(PrepareKrakenWithdrawalActivity activity);

    void inject(LoadedWalletActivity activity);

    void inject(VerifyCardActivity activity);

    void inject(EmptyWalletActivity activity);

}