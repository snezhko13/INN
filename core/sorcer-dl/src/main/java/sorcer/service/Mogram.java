/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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
import net.jini.id.Uuid;
import sorcer.core.context.ThrowableTrace;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 * A top-level interface for mograms - models of the {@link sorcer.service.modeling.Model} type
 * and routines of the {@link sorcer.service.Routine} type, or both.
 *
 * @author Mike Sobolewski
 */
public interface Mogram extends Identifiable, Contextion, Exertion, Discipline, Scopable, Substitutable, Arg {

    /**
     * Returns an ID of this mogram.
     *
     * @return a unique identifier
     */
    public Uuid getId();

    public void setId(Uuid id);

    /**
     * Generic federated execution called exertion by federated services.
     *
     * @param txn
     *            The transaction (if any) under which to exert.
     * @return a resulting exertion
     * @throws MogramException
     *             if a mogram error occurs
     * @throws RemoteException
     *             if remote call causes an error
     */
    public <T extends Contextion> T exert(Transaction txn, Arg... args) throws ContextException, RemoteException;

    public <T extends Contextion> T exert(Arg... args) throws ContextException, RemoteException;

    public int getIndex();

    public void setIndex(int i);

    public Contextion getParent();

    public void setParentId(Uuid parentId);

    public Signature getProcessSignature();

    public Mogram deploy(List<Signature> builders) throws MogramException, ConfigurationException;
    /**
     * Returns a status of this mogram.
     *
     * @return a status
     */
    public int getStatus();

    public void setStatus(int value);

    /**
     * Returns an execute of the component at the key
     *
     * @param key
     *            the component name
     * @return the componet at the path
     */
    public Object get(String key);

    public Mogram clearScope() throws MogramException;

	public Mogram clear() throws MogramException;

    public void reportException(Throwable t);

    /**
     * Returns the list of traces of thrown exceptions from this mogram.
     * @return ThrowableTrace list
     */
    public List<ThrowableTrace> getExceptions() throws RemoteException;

    public void reportException(String message, Throwable t);

    public void reportException(String message, Throwable t, ProviderInfo info);

    public void reportException(String message, Throwable t, Exerter provider);

    public void reportException(String message, Throwable t, Exerter provider, ProviderInfo info);

    /**
     * Returns the list of traces left by collborating services.
     * @return ThrowableTrace list
     */
    public List<String> getTrace() throws RemoteException;

    /**
     * Appends a trace info to a trace list of this mogram.
     */
    public void appendTrace(String info) throws RemoteException;

    /**
     * Returns the list of all traces of thrown exceptions with exceptions of
     * component mograms.
     *
     * @return ThrowableTrace list
     */
    public List<ThrowableTrace> getAllExceptions() throws RemoteException;


    /**
     * Returns a service fidelity of this exertion that consists of process
     * signature, all pre-processing, post-processing, and append signatures.
     * There is only one process signature defining late binding to the service
     * provider processing this exertion.
     *
     * @return a service fidelity
     * @param selection
     *            The service fidelity name.
     */
    public Fidelity selectFidelity(String selection) throws ConfigurationException;

    /**
     * Returns a service fidelity of this exertion that consists of process
     * signature, all pre-processing, post-processing, and append signatures.
     * There is only one process signature defining late binding to the service
     * provider processing this exertion.
     *
     * @return a collection of all service signatures
     * @see #getProcessSignature
     */
    public Fidelity getSelectedFidelity();

    /**
     * Returns service multi-fidelities of this exertion.
     */
    public Fi getMultiFi();

    /**
     * Returns a fdelity manager for of this exertion.
     */
    public FidelityManagement getFidelityManager();

    /**
     * Returns a fdelity manager for of this exertion.
     */
    public FidelityManagement getRemoteFidelityManager() throws RemoteException;

    /**
     * Returns <code>true</code> if this exertion should be monitored for its
     * execution, otherwise <code>false</code>.
     *
     * @return <code>true</code> if this exertion requires its execution to be
     *         monitored.
     */
    public boolean isMonitorable() throws RemoteException;

    /**
     * The exertion format for thin exertions (no RMI and Jini classes)
     */
    public static final int THIN = 0;

    public Uuid getParentId();

    /**
     * Return date when exertion was created
     * @return
     */
    public Date getCreationDate();

	public Date getGoodUntilDate();

	/**
	 * @param date
	 *            The goodUntilDate to set.
	 */
	public void setGoodUntilDate(Date date);

	public String getDomainId();

	/**
	 * @param id
	 *            The domainID to set.
	 */
	public void setDomainId(String id);

	public String getSubdomainId();

	/**
	 * @param id
	 *            The subdomainID to set.
	 */
	public void setSubdomainId(String id);

	/**
	 */
	public String getDomainName();

	/**
	 * @param name
	 *            The domain name to set.
	 */
	public void setDomainName(String name);

    /**
     * Return a subdomain name
     */
	public String getSubdomainName();


    /**
     * Returns a ebalated value at teh path
     */
    public Object getEvaluatedValue(String path) throws ContextException;

    /**
     * Returns the state of this mogram evaluation
     */
    public boolean isEvaluated();

        /**
         * @param name
         *            The subdomainName to set.
         */
    public void setSubdomainName(String name);

    /**
     * Returns a principal using this service context.
     *
     * @return a Principal
     */
    public Principal getPrincipal();

    /**
     */
    public Date getLastUpdateDate();

    /**
     * @param date
     *            The lastUpdateDate to set.
     */
    public void setLastUpdateDate(Date date);

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description);

    /** 
     *
     * Assigns a name for this service context. 
     *
     * @param name 
     *       a context name to set.
     */

    public void setName(String name);

    /**
     */
    public String getDescription();

    /**
     */
    public String getOwnerId();

    /**
     */
    public String getSubjectId();

    /**
     * @param projectName
     *            The project to set.
     */
    public void setProjectName(String projectName);

    /**
     */
    public String getProjectName();

    public boolean isValid();

    public void setValid(boolean state);

    /**
     * Returns a data service context (service data) of this mogram.
     *
     * @return a service context
     * @throws ContextException
     */
    public Context getDataContext() throws ContextException;

    /**
     * Reconfigure this mogram with given fudelities.
     *
     * @param fidelities
     */
    public void reconfigure(Fidelity... fidelities) throws ContextException, RemoteException, ConfigurationException;

    /**
     * Reconfigure this mmogramodel with given names of metafidelities.
     *
     * @param metaFiNames
     */
    public void morph(String... metaFiNames) throws ContextException, RemoteException, ConfigurationException;

    /**
     * Update this mogram with given setup context entries.
     *
     * @param contextEntries
     */
    public void update(Setup... contextEntries) throws ContextException, RemoteException;

    /**
     * Returns the first fidelity name of a given projection.
     *
     * @param projectionName
     */
    public String getProjectionFi(String projectionName) throws ContextException, RemoteException;

    /**
     * Check if this context is export controlled, accessible to principals from
     * export controlled countries.
     *
     * @return true if is export controlled
     */
    public boolean isExportControlled();

	List<Discipline> getMograms(List<Discipline> allMograms);

	List<Contextion> getContextions(List<Contextion> allContextions);

	/**
	 * Returns the list of direct component exertions.
	 * @return Routine list
	 */
	public List<Discipline> getMograms();

	public List<Contextion> getContextions();


	/**
	 * Returns the list of all nested component exertions/
	 * @return Routine list
	 */
	public List<Discipline> getAllMograms();

	public List<Contextion> getAllContextions();

	/**
     *  Returns a signature builder that returns instances of this model.
     *  A inConnector specifies a map of an input context as needed by another collaborating service.
     *
     * @param args  optional configuration arguments
     * @return  a signature for the builder of this model                                                                                s
     * @throws ContextException
     * @throws RemoteException
     */
    public Signature getBuilder(Arg... args) throws MogramException;

    public void applyFidelity(String name);

    public void setBuilder(Signature builder) throws MogramException;

	/**
	 * Returns true if this exertion is a branching or looping exertion.
	 */
	public boolean isConditional();

	/**
	 * Returns true if this exertion is composed of other exertions.
	 */
	public boolean isCompound();

    /**
     * Returns true if this exertion is executable in Collaboration
     */
    public boolean isExec();

    public void setExec(boolean exec);
}
