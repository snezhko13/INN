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

package sorcer.core.context.model.ent;

import sorcer.service.EvaluationException;

public class EntException extends EvaluationException {

	private static final long serialVersionUID = 5987271488623420213L;
	private String parName = null;
	private Exception exception = null;
	
	public EntException() {
	}
	/**
	 * Constructs a new EntException with an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public EntException(Exception exception) {
		super(exception);
	}
	
	public EntException(String msg, Exception e) {
		super(msg);
		e.printStackTrace();
	}

	public EntException(String msg) {
		super(msg);
	}
	
	public EntException(String msg, String parName, Exception exception) {
		super(msg);
		this.parName = parName;
		this.exception = exception;
	}
	
	public String getVarName() {
		return parName;
	}
	
	public Exception getException() {
		return exception;
	}
}
