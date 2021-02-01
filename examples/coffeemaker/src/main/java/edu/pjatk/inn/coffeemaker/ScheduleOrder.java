package edu.pjatk.inn.coffeemaker;

import sorcer.service.Context;
import sorcer.service.ContextException;
import java.rmi.RemoteException;

public interface ScheduleOrder {

    public Context schedule(Context context) throws RemoteException, ContextException;

}
