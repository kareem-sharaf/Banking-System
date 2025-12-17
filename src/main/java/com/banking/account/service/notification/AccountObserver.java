package com.banking.account.service.notification;

public interface AccountObserver {

    void update(AccountEvent event);

    String getObserverType();
}
