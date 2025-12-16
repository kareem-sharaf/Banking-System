package com.banking.observer;

import com.banking.observer.event.AccountEvent;

public interface AccountObserver {

    void update(AccountEvent event);

    String getObserverType();
}
