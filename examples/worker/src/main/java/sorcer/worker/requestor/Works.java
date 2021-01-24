package sorcer.worker.requestor;

import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Works implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static Work work0, work1, work2, work3, work4;

	static {
		
		 work0 = new Work() {
			private static final long serialVersionUID = 1L;

			public Context<Integer> exec(Context cxt) throws InvalidWork, ContextException, RemoteException {
				int arg1 = (Integer)value(cxt, "fxn/arg/1");
				int arg2 = (Integer)value(cxt, "fxn/arg/2");
				int result =  arg1 * arg2;
				put(cxt, "prv/result", result);
				cxt.setReturnValue(result);
				return cxt;
			}
		};
		
		work1 = new Work() {
			private static final long serialVersionUID = 1L;

			public Context exec(Context cxt) throws ContextException, RemoteException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "fxn/arg/1";
				String operand2Path = "fxn/arg/2";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			     } else {
			    	 sigPrefix = ""; 
			     }	        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int result = arg1 + arg2;
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if (outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					String[] mpaths = cxt.getMarkedPaths("tag|"+sigPrefix);
				    if (mpaths.length == 1) {
				        cxt.putOutValue(mpaths[0], result);
				    }
				} else
					cxt.putOutValue("prv/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};

		work2 = new Work() {
			private static final long serialVersionUID = 1L;

			public Context exec(Context cxt) throws ContextException, RemoteException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "fxn/arg/1";
				String operand2Path = "fxn/arg/2";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			     } else {
			    	 sigPrefix = ""; 
			     }			        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int result = arg1 * arg2;
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					String[] mpaths = cxt.getMarkedPaths("tag|"+sigPrefix);
				    if (mpaths.length == 1) {
				        cxt.putOutValue(mpaths[0], result);
				    }
				} else
					cxt.putOutValue("prv/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};

		work3 = new Work() {
			private static final long serialVersionUID = 1L;

			public Context exec(Context cxt) throws ContextException, RemoteException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "fxn/arg/1";
				String operand2Path = "fxn/arg/2";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			     } else {
			    	 sigPrefix = ""; 
			     }
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int result = arg2 - arg1;
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					String[] mpaths = cxt.getMarkedPaths("tag|"+sigPrefix);
				    if (mpaths.length == 1) {
				        cxt.putOutValue(mpaths[0], result);
				    }
				} else
					cxt.putOutValue("prv/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};
		
		work4 = new Work() {
			private static final long serialVersionUID = 1L;

			public Context exec(Context cxt) throws ContextException, RemoteException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "fxn/arg/1";
				String operand2Path = "fxn/arg/2";
				String operand3Path = "fxn/arg/3";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			    	 operand2Path = sigPrefix + "/" + operand3Path;
			     } else {
			    	 sigPrefix = ""; 
			     }	        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int arg3 = (Integer) cxt.getValue(operand3Path);
				int result = Math.round((arg1 + arg2 + arg3)/3);
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					String[] mpaths = cxt.getMarkedPaths("tag|"+sigPrefix);
				    if (mpaths.length == 1) {
				        cxt.putOutValue(mpaths[0], result);
				    }
				} else
					cxt.putOutValue("prv/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};
		
	}
}
