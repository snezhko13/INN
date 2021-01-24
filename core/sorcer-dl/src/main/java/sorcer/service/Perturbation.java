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

import java.rmi.RemoteException;

public interface  Perturbation <T extends Object> {

	/**
	 * Returns the execute of the associated perturbed state.
	 * 
	 * @return the current execute of this state
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public T getPerturbedValue(String varName) throws EvaluationException, RemoteException, ConfigurationException;
	
	public double getPerturbation();
}
