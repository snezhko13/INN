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

package sorcer.core.exertion;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.MethodInvoker;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.core.signature.LocalSignature;
import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * The SORCER object block extending the basic block implementation {@link Block}.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectBlock extends Block {
	
	private static final long serialVersionUID = -3917210130874207557L;
	
	public ObjectBlock() throws SignatureException {
		this("object block-" + count++);
	}
	
	public ObjectBlock(String name) throws SignatureException {
		super(name, new LocalSignature("exert", ServiceConcatenator.class));
	}

	public ObjectBlock(String name, Context context)
			throws SignatureException {
		this(name);
		if (context != null)
			this.dataContext = (ServiceContext) context;
	}
	
	public Block doBlock(Transaction txn, Arg... args) throws RoutineException,
			SignatureException, RemoteException {
		// return (Job) new ServiceJobber().execEnt(job, txn);
		Block result = null;
		try {
			LocalSignature os = (LocalSignature) getProcessSignature();
			Evaluator evaluator = ((LocalSignature) getProcessSignature())
					.getEvaluator();
			if (evaluator == null) {
				evaluator = new MethodInvoker(os.newInstance(),
						os.getSelector());
			}
			evaluator.setParameterTypes(new Class[] { Mogram.class });
			evaluator.setParameters(new Object[] { this });
			result = (Block)evaluator.evaluate(args);
			getControlContext().appendTrace("block by: " + evaluator.getClass().getName());
		} catch (Exception e) {
			e.printStackTrace();
			if (controlContext != null)
				controlContext.addException(e);
		}
		return result;
	}
}
