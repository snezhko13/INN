package sorcer.worker.requestor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.RemoteSignature;
import sorcer.service.Context;
import sorcer.service.Routine;
import sorcer.service.Job;
import sorcer.service.Task;
import sorcer.util.Sorcer;
import sorcer.worker.provider.Worker;

import java.net.InetAddress;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class WorkerJobApplication {

	private final static Logger logger = LoggerFactory.getLogger(WorkerJobApplication.class);

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		// initialize system properties
		Sorcer.getEnvProperties();
		
		// getValue the queried provider key from the command line
		String pn1 = args[0];
		String pn2 = args[1];
		String pn3 = args[2];
		
		logger.info("Provider name1: " + pn1);
		logger.info("Provider name2: " + pn2);
		logger.info("Provider name3: " + pn3);

		Routine result = new WorkerJobApplication()
			.getExertion(pn1, pn2, pn3).exert();
		// getValue contexts of component domains - in this case tasks
		logger.info("Output context1: \n" + result.getContext("work1"));
		logger.info("Output context2: \n" + result.getContext("work2"));
		logger.info("Output context3: \n" + result.getContext("work3"));
	}

	private Routine getExertion(String pn1, String pn2, String pn3) throws Exception {
        String hostname = InetAddress.getLocalHost().getHostName();

        if (pn1!=null) pn1 = Sorcer.getActualName(pn1);
        if (pn2!=null) pn2 = Sorcer.getActualName(pn2);
        if (pn3!=null) pn3 = Sorcer.getActualName(pn3);

        Context context1 = new ServiceContext("work1");
        context1.putValue("requstor/key", hostname);
        context1.putValue("consumer/operand/1", 1);
        context1.putValue("consumer/operand/2", 1);
        context1.putValue("to/provider/key", pn1);
        context1.putValue("consumer/work", Works.work1);

        Context context2 = new ServiceContext("work2");
        context2.putValue("requstor/key", hostname);
        context2.putValue("consumer/operand/1", 2);
        context2.putValue("consumer/operand/2", 2);
        context2.putValue("to/provider/key", pn2);
        context2.putValue("consumer/work", Works.work2);

        Context context3 = new ServiceContext("work3");
        context3.putValue("requstor/key", hostname);
        context3.putValue("consumer/operand/1", 3);
        context3.putValue("consumer/operand/2", 3);
        context3.putValue("to/provider/key", pn3);
        context3.putValue("consumer/work", Works.work3);

		RemoteSignature signature1 = new RemoteSignature("doWork", Worker.class, pn1);
		RemoteSignature signature2 = new RemoteSignature("doWork", Worker.class, pn2);
		RemoteSignature signature3 = new RemoteSignature("doWork", Worker.class, pn3);
		
		Task task1 = new NetTask("work1", signature1, context1);
		Task task2 = new NetTask("work2", signature2, context2);
		Task task3 = new NetTask("work3", signature3, context3);
		Job job = new NetJob();
		job.addMogram(task1);
		job.addMogram(task2);
		job.addMogram(task3);
		return job;
	}
}
