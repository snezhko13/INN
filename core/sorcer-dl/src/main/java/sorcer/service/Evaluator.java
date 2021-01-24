/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;


/**
 * A functionality required by all evaluators in SORCER.
 * 
 * @author Mike Sobolewski
 */
public interface Evaluator <T> extends Opservice, Exertion, Provider, Evaluation<T>, Scopable, Getter<T>, Activity, Identifiable {

	public void addArgs(ArgSet set) throws EvaluationException, RemoteException;
	
	public ArgSet getArgs();
	
	public void setParameterTypes(Class<?>[] types);
	
	public void setParameters(Object... args);

	public void update(Setup... entries) throws ContextException;

	public void setNegative(boolean negative);

	public void setValid(boolean state);

	public boolean isValid();

	public boolean isChanged();

	public enum SPI {
		JEP, GROOVY, METHOD, EXERTION, OBJECT, PROXY, INDEPENDENT, DEPENDENT, SOA, NULL
	}
	/**
	 * Returns a Context.Return to the return execute by this signature.
	 *
	 * @return Context.Return to the return execute
	 */
	public Context.Return getContextReturn();
}
