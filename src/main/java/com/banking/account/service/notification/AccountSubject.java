package com.banking.account.service.notification;

public interface AccountSubject {

    void attach(AccountObserver observer);

    void detach(AccountObserver observer);

    void notifyObservers(AccountEvent event);
}
