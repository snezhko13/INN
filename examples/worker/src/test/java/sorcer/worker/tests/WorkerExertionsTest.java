package sorcer.worker.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.ent.operator;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.worker.provider.Worker;
import sorcer.worker.provider.impl.WorkerBean;
import sorcer.worker.provider.impl.WorkerProvider;
import sorcer.worker.requestor.Works;

import java.net.InetAddress;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/worker")
public class WorkerExertionsTest {
	private final static Logger logger = LoggerFactory.getLogger(WorkerExertionsTest.class);

	static private Context context;
	static private String hostname;
	static private URL contextUrl;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		hostname = InetAddress.getLocalHost().getHostName();
		
		context = context("work", 
				operator.ent("fxn/key", hostname),
				operator.ent("fxn/arg/1", 11),
				operator.ent("fxn/arg/2", 101),
				operator.ent("fxn/work", Works.work0),
				operator.ent("to/prv/key", "SORCER Worker"));
		
		contextUrl = store(context);
	}

	@Test
	public void localProviderTest() throws Exception {

		Task pt = task("work", sig("doWork", WorkerProvider.class), 
				context);
		Task et = exert(pt);
	
		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/host/key"), hostname);
		
	}

	@Test
	public void beanTest() throws Exception {

		Task pt = task("work", sig("doWork", WorkerBean.class),
				context);
		Task et = exert(pt);

		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/host/key"), Context.none);

	}

	@Test
	public void dbContextTest() throws Exception {

		Task pt = task("work", sig("doWork", WorkerProvider.class), 
				context("work", contextUrl));
		Task et = exert(pt);
	
		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/host/key"), hostname);
		
	}
	
	@Test
	public void remoteProviderTest() throws Exception {

		Task pt = task("work", sig("doWork", Worker.class, prvName("Worker1")), 
				context(contextUrl));
		Task et = exert(pt);
		
		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/message"), "Done work by: " + actualName("Worker1"));
		assertEquals(get(et, "prv/host/key"), hostname);
		
	}
	
	
	@Test
	public void sharedTaskContext() throws Exception {

		// prefixed task context and output values
		context = context("pBatch",
					inVal("w1/fxn/arg/1", 20), inVal("w1/fxn/arg/2", 80),
					inVal("w1/fxn/work", Works.work1), outVal("fxn/arg/1", "tag|w1"),

					inVal("w2/fxn/arg/1", 10), inVal("w2/fxn/arg/2", 50),
					inVal("w2/fxn/work", Works.work2), outVal("fxn/arg/2", "tag|w2"),

					inVal("fxn/work", Works.work3), outVal("prv/result"));


		Task bt = task("pBatch", 
					type(sig("doWork#w1", Worker.class, prvName("Worker1"),
							result("fxn/arg/1")), Signature.PRE),
					type(sig("doWork#w2", Worker.class, prvName("Worker2"),
							result("fxn/arg/2")), Signature.PRE),
					sig("doWork", Worker.class, prvName("Worker3"),
							result("prv/result")),
					context);

		requestTime(bt);
		Task out = exert(bt);
		logger.info("task context: " + context(out));
		assertEquals(get(out, "prv/result"), 400);
	}


	@Test
	public void contextPipes() throws Exception {
		
		Context cxt1 = context(operator.ent("fxn/key", "workaholic"),
				operator.ent("fxn/arg/1", 20),  operator.ent("fxn/arg/2", 80),
				operator.ent("fxn/work", Works.work1),  operator.ent("tp/prv/key", "Worker1"),
				outVal("prv/result"));

		Context cxt2 = context(operator.ent("fxn/key", "workaholic"),
				operator.ent("fxn/arg/1", 10),  operator.ent("fxn/arg/2", 50),
				operator.ent("fxn/work", Works.work2),  operator.ent("tp/prv/key", "Worker2"),
				outVal("prv/result"));

		Context cxt3 = context(operator.ent("fxn/key", "workaholic"),
				inVal("fxn/arg/1"),  inVal("fxn/arg/2"),
				operator.ent("fxn/work", Works.work3),  operator.ent("tp/prv/key", "Worker3"),
				outVal("prv/result"));

		Job job = job("strategy", 
				task("work1", sig("doWork", Worker.class, prvName("Worker1")), cxt1),
				task("work2", sig("doWork", Worker.class, prvName("Worker2")), cxt2),
				task("work3", sig("doWork", Worker.class, prvName("Worker3")), cxt3),
				pipe(outPoint("strategy/work1", "prv/result"), inPoint("strategy/work3", "fxn/arg/1")),
				pipe(outPoint("strategy/work2", "prv/result"), inPoint("strategy/work3", "fxn/arg/2")));

		requestTime(job);
		Job out = exert(job);
		logger.info("job context: " + upcontext(out));
		assertEquals(get(out, "strategy/work3/prv/result"), 400);
		
	}

	@Test
	public void exertionStrategy() throws Exception {

		Context cxt1 = context(operator.ent("fxn/key", "workaholic"),
				operator.ent("fxn/arg/1", 20),  operator.ent("fxn/arg/2", 80),
				operator.ent("fxn/work", Works.work1),  operator.ent("tp/prv/key", "Worker1"));

		Context cxt2 = context(operator.ent("fxn/key", "workaholic"),
				operator.ent("fxn/arg/1", 10),  operator.ent("fxn/arg/2", 50),
				operator.ent("fxn/work", Works.work2),  operator.ent("tp/prv/key", "Worker2"));

		Context cxt3 = context(operator.ent("fxn/key", "workaholic"),
				operator.ent("fxn/arg/1", 100),  operator.ent("fxn/arg/2", 100),
				operator.ent("fxn/work", Works.work3),  operator.ent("tp/prv/key", "Worker3"));

		Job job = job("strategy", 
				task("work1", sig("doWork", Worker.class, prvName("Worker1")), cxt1),
				task("work2", sig("doWork", Worker.class, prvName("Worker2")), cxt2),
				task("work3", sig("doWork", Worker.class, prvName("Worker3")), cxt3));

		Strategy control1 = strategy(Flow.SEQ, Access.PUSH);
		Strategy control2 = strategy(Flow.PAR, Access.PUSH);
		Strategy control3 = strategy(Flow.SEQ, Access.PULL);
		Strategy control4 = strategy(Flow.PAR, Access.PULL);

		Job out = exert(job, control1);
		logger.info("job context: " + upcontext(out));
		logger.info("control context: " + control(job));
		assertEquals(get(out, "strategy/work1/prv/result"), 100);
		assertEquals(get(out, "strategy/work2/prv/result"), 500);
		assertEquals(get(out, "strategy/work3/prv/result"), 0);
		
		out = exert(job, control2);
		logger.info("job context: " + upcontext(out));
		logger.info("control context: " + control(job));
		assertEquals(get(out, "strategy/work1/prv/result"), 100);
		assertEquals(get(out, "strategy/work2/prv/result"), 500);
		assertEquals(get(out, "strategy/work3/prv/result"), 0);
		
		out = exert(job, control3);
		logger.info("job context: " + upcontext(out));
		logger.info("control context: " + control(job));
		assertEquals(get(out, "strategy/work1/prv/result"), 100);
		assertEquals(get(out, "strategy/work2/prv/result"), 500);
		assertEquals(get(out, "strategy/work3/prv/result"), 0);
		
		out = exert(job, control4);
		logger.info("job context: " + upcontext(out));
		logger.info("control context: " + control(job));
		assertEquals(get(out, "strategy/work1/prv/result"), 100);
		assertEquals(get(out, "strategy/work2/prv/result"), 500);
		assertEquals(get(out, "strategy/work3/prv/result"), 0);

	}
	
}
