package com.tangem.DI;

import com.tangem.AppController;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(AppController appController);

}
