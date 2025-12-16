package com.banking.observer;

import com.banking.observer.event.AccountEvent;

public interface AccountSubject {

    void attach(AccountObserver observer);

    void detach(AccountObserver observer);

    void notifyObservers(AccountEvent event);
}
