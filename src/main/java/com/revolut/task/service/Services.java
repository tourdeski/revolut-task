package com.revolut.task.service;

import org.jetbrains.annotations.NotNull;

/**
 * All existing REST API services
 */
public enum Services {

    ACCOUNT_SERVICE(new AccountService()),
    //
    ;

    @NotNull
    private RemoteService service;

    @NotNull
    public RemoteService getServiceInstance() {
        return service;
    }

    Services(@NotNull RemoteService service) {
        this.service = service;
    }
}
