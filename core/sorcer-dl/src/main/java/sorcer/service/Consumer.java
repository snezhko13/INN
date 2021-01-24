/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.service;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Mike Sobolewski on 08/14/18.
 *
 * Execute provided list of services as a service pipe.
 */
public interface Consumer extends Service, Client {

    Context consume(Context context, Arg[] args) throws ServiceException, RemoteException, TransactionException;

    List<Service> getService(String... args) throws ServiceException;

    Transaction getTransaction();

    void preprocess(String... args) throws MogramException, ContextException;

    void process(String... args) throws MogramException, ContextException;

    void postprocess(String... args) throws MogramException, ContextException;

}
