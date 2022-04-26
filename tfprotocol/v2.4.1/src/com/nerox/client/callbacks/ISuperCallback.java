package com.nerox.client.callbacks;

import com.nerox.client.TfprotocolSuper;
import com.nerox.client.misc.StatusInfo;

/**
 * The callback system where payload will be notified to the user for his own manipulation...
 * */
public interface ISuperCallback <T extends TfprotocolSuper>{
    default void statusServer(StatusInfo status){
        throw new RuntimeException("Callback is not implemented: exception");
    }
    void responseServerCallback(StatusInfo status);
    void instanceTfProtocol(T instance);
}
