package com.tangem.di;

import com.tangem.presentation.activity.LogoActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {NavigatorModule.class})
public interface NavigatorComponent {

    void inject(LogoActivity activity);

}