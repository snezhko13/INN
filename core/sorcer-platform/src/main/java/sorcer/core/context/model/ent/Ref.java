/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
package sorcer.core.context.model.ent;

import sorcer.service.Arg;
import sorcer.service.Context;
import sorcer.service.EvaluationException;
import sorcer.service.modeling.Reference;
import sorcer.service.modeling.SupportComponent;

import java.rmi.RemoteException;

/**
 * In service mogrogramming, a reference (for short a ref) is a special kind of
 * entry, used as a handle to its referenced value. While the referenced value is not
 * defined it has to be bound the its referenced value at runtime.
 * 
 * @author Mike Sobolewski
 */
public class Ref<T> extends Entry<T> implements Reference<T>, SupportComponent {

	private static final long serialVersionUID = 1L;

	private Arg[] args = new Arg[0];

	public Ref() {
		super();
	}

	public Ref(final String path, Arg... args) {
		if(path==null)
			throw new IllegalArgumentException("contextReturn must not be null");
		this.key = path;
		this.args = args;

	}

	public Ref(final String path, Context scope, Arg... args) {
		if(path==null)
			throw new IllegalArgumentException("contextReturn must not be null");
		this.key = path;
		this.scope = scope;
		this.args = args;
	}

	@Override
	public T getValue(Arg... args) {
		if (out == null) {
			// resolve reference
			out = (T) scope.asis(key);
		}
		return out;
	}

	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		return getValue(args);
	}
}
