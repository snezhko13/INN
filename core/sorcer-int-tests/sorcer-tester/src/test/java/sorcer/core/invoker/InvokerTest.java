package sorcer.core.invoker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.volume.Volume;
import sorcer.core.context.model.ent.EntryModel;
import sorcer.core.context.model.ent.Prc;
import sorcer.ent.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils.CmdResult;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.ent.operator.alt;
import static sorcer.ent.operator.*;
import static sorcer.ent.operator.get;
import static sorcer.ent.operator.loop;
import static sorcer.ent.operator.opt;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class InvokerTest {
	private final static Logger logger = LoggerFactory.getLogger(InvokerTest.class);

	private EntryModel pm;
	private Prc x;
	private Prc y;
	private Prc z;

	/// member subclass of Updater with Context parameter used below with
	// contextMethodAttachmentWithArgs()
	// there are constructor's context and invoke metod's context as parameters
	public class ContextUpdater extends Updater {
		public ContextUpdater(Context context) {
			super(context);
		}

		public Double update(Context arg) throws Exception {
			setValue(x, value(arg, "x"));
			setValue(y, value(context, "y"));
			logger.info("update x: " + exec(x));
			logger.info("update y: " + exec(y));
			return (double)exec(x) + (double)exec(y) + (double)exec(pm, "z");
		}
	};

	@Before
	public void initProcModel() throws Exception {
		pm = new EntryModel();
		x = prc("x", 10.0);
		y = prc("y", 20.0);
		z = prc("z", invoker("x - y", x, y));
	}

	@Test
	public void lambdaInvoker() throws RemoteException, ContextException,
			SignatureException, RoutineException {
		Evaluator invoker = invoker("fxn",
				cxt ->  (double) value(cxt, "x") + (double) value(cxt, "y") + 30,
				context(val("x", 10.0), val("y", 20.0)),
				args("x", "y"));
		logger.info("invoke eval: " + invoke(invoker));
		assertEquals(invoke(invoker), 60.0);
	}

	@Test
	public void methodInvokerTest() throws Exception {
		setValue(x, 10.0);
		setValue(y, 20.0);
		add(pm, x, y, z);

//		logger.info("x:" + eval(pm, "x"));
//		logger.info("y:" + eval(pm, "y"));
//		logger.info("y:" + eval(pm, "z"));

		Context in = context(val("x", 20.0), val("y", 30.0));
		Context arg = context(val("x", 200.0), val("y", 300.0));
		add(pm, methodInvoker("update", new ContextUpdater(in), arg));
		logger.info("prc eval:" + invoke(pm, "update"));
		assertEquals(exec(pm, "update"), 400.0);
	}

	@Test
	public void groovyInvokerTest() throws RemoteException, ContextException {
		EntryModel pm = entModel("prc-model");
		add(pm, prc("x", 10.0), prc("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", args("x", "y")));
		logger.info("invoke eval: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
		logger.info("getValue eval: " + exec(pm, "expr"));
		assertEquals(exec(pm, "expr"), 60.0);
	}

	@Test
	public void lambdaInvokerTest() throws RemoteException, ContextException {
		EntryModel pm = entModel("model");
		add(pm, prc("x", 10.0), prc("y", 20.0));
		add(pm, invoker("fxn", cxt -> (double)value(cxt, "x") + (double)value(cxt, "y") + 30));
		logger.info("invoke eval: " + invoke(pm, "fxn"));
		assertEquals(invoke(pm, "fxn"), 60.0);
		logger.info("getValue eval: " + value(pm, "fxn"));
		assertEquals(exec(pm, "fxn"), 60.0);
	}

	@Test
	public void lambdaInvokerTest2() throws Exception {

		Model mo = model(prc("x", 10.0), prc("y", 20.0),
				operator.prc(invoker("fxn", cxt -> (double) value(cxt, "x")
									+ (double) value(cxt, "y")
									+ 30)));
		logger.info("invoke eval: " + eval(mo, "fxn"));
		assertEquals(exec(mo, "fxn"), 60.0);
	}

	@Test
	public void lambdaInvokerTest3() throws Exception {


		Context scope = context(prc("x1", 20.0), prc("y1", 40.0));

		Model mo = model(prc("x", 10.0), prc("y", 20.0),
			operator.prc(invoker("fxn", (cxt) -> {
						return (double) value(cxt, "x")
								+ (double) value(cxt, "y")
								+ (double) value(cxt, "y1")
								+ 30;
					},
				scope,
				args("x", "y", "y1"))));
		logger.info("invoke eval: " + eval(mo, "fxn"));
		assertEquals(exec(mo, "fxn"), 100.0);
	}

	@Test
	public void invokeProcTest() throws RemoteException, ContextException,
			SignatureException, RoutineException {

		Prc x1 = prc("x1", 1.0);
		// logger.info("invoke eval:" + invoke(x1));
		assertEquals(exec(x1), 1.0);
	}

	@Test
	public void invokeProcArgTest() throws RemoteException, ContextException,
			SignatureException, RoutineException {
		Prc x1, x2, y;
		x1 = prc("x1", 1.0);
		x2 = prc("x2", 2.0);
		y = prc("y", invoker("x1 + x2", args("x1", "x2")));

		Object out = exec(y, prc("x1", 10.0), prc("x2", 20.0));
//		logger.info("y: " + out);
		assertTrue(out.equals(30.0));
	}

	@Test
	public void cmdInvokerTest() throws SignatureException, RoutineException,
			ContextException, IOException {
		String riverVersion = System.getProperty("river.version");
		String sorcerVersion = System.getProperty("sorcer.version");
		String slf4jVersion = System.getProperty("slf4j.version");
		String logbackVersion = System.getProperty("logback.version");
		String buildDir = System.getProperty("project.build.dir");

		String cp = buildDir + "/libs/sorcer-tester-" + sorcerVersion + ".jar" + File.pathSeparator
				+ Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";


		ServiceInvoker cmd = cmdInvoker("volume",
				"java -cp  " + cp + Volume.class.getName() + " cylinder");

		EntryModel pm = entModel(operator.prc(cmd),
				ent("x", 10.0), ent("y"),
				ent("multiply", invoker("x * y", args("x", "y"))),
				ent("add", invoker("x + y", args("x", "y"))));

		CmdResult result = (CmdResult) invoke(pm, "volume");
		// getValue from the result the volume of cylinder and assign to y parameter
		assertTrue("EXPECTED '0' return eval, GOT: "+result.getExitValue(), result.getExitValue() == 0);
		Properties props = new Properties();
		props.load(new StringReader(result.getOut()));

		setValue(pm, "y", new Double(props.getProperty("cylinder/volume")));

		logger.info("x eval:" + value(pm, "x"));
		logger.info("y eval:" + value(pm, "y"));
		logger.info("multiply eval:" + value(pm, "add"));
		assertEquals(value(pm, "add"), 47.69911184307752);
	}

	@Test
	public void conditionalInvoker() throws RemoteException, ContextException {
		final EntryModel pm = new EntryModel("prc-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
        pm.putValue("condition", invoker("x > y", args("x", "y")));

		//pm.putValue("condition", new ServiceInvoker(pm));
		
		//((ServiceInvoker) pm.getValue("condition")).setArgs(args("x", "y")).setEvaluator(
		//		invoker("x > y"));

		
		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		
		Object val = pm.getValue("condition");
//		logger.info("condition eval: " + val);
//		logger.info("condition eval: " + pm.execute("condition"));
		assertEquals(pm.getValue("condition"), false);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		// logger.info("condition eval: " + pm.execute("condition"));
		assertEquals(pm.getValue("condition"), true);

		// enclosing class conditional context
		Condition c = new Condition() {
			@Override
			public boolean isTrue() throws ContextException {
				return (Boolean) pm.getValue("condition");
			}
		};
		assertEquals(c.isTrue(), true);

		// provided conditional context
		Condition eval = new Condition(pm) {
			@Override
			public boolean isTrue() throws ContextException {
				try {
					return (Boolean) conditionalContext.getValue("condition");
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			}
		};
		assertEquals(eval.evaluate(), true);
	}

	@Test
	public void optInvokerTest1() throws RemoteException, ContextException {
		EntryModel pm = new EntryModel("prc-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				"{ x, y -> x > y }", "x", "y"), invoker("x + y",
				args("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		assertEquals(opt.evaluate(), null);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt eval: " + opt.evaluate());
		assertEquals(opt.evaluate(), 500.0);
	}

	@Test
	public void optInvokerTest2() throws RemoteException, ContextException {
		// Java 8 lambdas style
		EntryModel pm = new EntryModel("prc-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				cxt -> (double)v(cxt, "x") > (double)v(cxt, "y")), invoker("x + y",
				args("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		assertEquals(opt.evaluate(), null);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt eval: " + opt.evaluate());
		assertEquals(opt.evaluate(), 500.0);
	}

	@Test
	public void smlOptInvokerTest() throws RemoteException, ContextException {
		EntryModel pm = entModel("prc-model");
		add(pm,
				val("x", 10.0),
				val("y", 20.0),
				opt("opt", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y", args("x", "y"))));

		logger.info("opt exec: " + exec(pm, "opt"));
		assertEquals(exec(pm, "opt"), null);

		setValues(pm, val("x", 300.0), val("y", 200.0));
		logger.info("opt eval: " + exec(pm, "opt"));
		assertEquals(exec(pm, "opt"), 500.0);
	}

	@Test
	public void altInvokerTest() throws RemoteException, ContextException {
		EntryModel pm = new EntryModel("prc-model");
		pm.putValue("x", 30.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		pm.putValue("x3", 70.0);
		pm.putValue("y3", 60.0);

		OptInvoker opt1 = new OptInvoker("opt1", condition(pm,
				"{ x, y -> x > y }", "x", "y"), invoker("x + y + 10",
				args("x", "y")));

		OptInvoker opt2 = new OptInvoker("opt2", condition(pm,
				"{ x2, y2 -> x2 > y2 }", "x2", "y2"), invoker(
				"x + y + 20", args("x", "y")));

		OptInvoker opt3 = new OptInvoker("op3", condition(pm,
				"{ x3, y3 -> x3 > y3 }", "x3", "y3"), invoker(
				"x + y + 30", args("x", "y")));

		// no condition means condition(true)
		OptInvoker opt4 = new OptInvoker("opt4", invoker("x + y + 40",
				args("x", "y")));

		AltInvoker alt = new AltInvoker("alt", opt1, opt2, opt3, opt4);
		add(pm, opt1, opt2, opt3, opt4, alt);

		logger.info("opt1 eval: " + eval(opt1));
		assertEquals(eval(opt1), 60.0);
		logger.info("opt2 eval: " + eval(opt2));
		assertEquals(eval(opt2), 70.0);
		logger.info("opt3 eval: " + eval(opt3));
		assertEquals(eval(opt3), 80.0);
		logger.info("opt4 eval: " + eval(opt4));
		assertEquals(eval(opt4), 90.0);
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 60.0);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt eval: " + eval(alt));
		assertEquals(eval(alt), 510.0);

		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 40.0);
		pm.putValue("y2", 50.0);
		pm.putValue("x3", 50.0);
		pm.putValue("y3", 60.0);
		logger.info("opt eval: " + alt.evaluate());
		assertEquals(eval(alt), 70.0);

		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		logger.info("opt eval: " + alt.evaluate());
		assertEquals(eval(alt), 50.0);
	}

	@Test
	public void multiOptAltInvokerTest() throws RemoteException, ContextException {
		EntryModel pm = entModel("prc-model");
		// add(pm, entry("x", 10.0), entry("y", 20.0), prc("x2", 50.0),
		// prc("y2", 40.0), prc("x3", 50.0), prc("y3", 60.0));
		add(pm, val("x", 10.0), val("y", 20.0), val("x2", 50.0),
				val("y2", 40.0), val("x3", 50.0), val("y3", 60.0));

		AltInvoker alt = alt(
				"alt",
				opt("opt1", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y + 10", args("x", "y"))),
				opt("opt2", condition(pm, "{ x2, y2 -> x2 > y2 }", "x2", "y2"),
						invoker("x + y + 20", args("x", "y"))),
				opt("opt3", condition(pm, "{ x3, y3 -> x3 > y3 }", "x3", "y3"),
						invoker("x + y + 30", args("x", "y"))),
				opt("opt4", invoker("x + y + 40", args("x", "y"))));

		add(pm, alt, get(alt, 0), get(alt, 1), get(alt, 2), get(alt, 3));

		logger.info("opt1 exec : " + exec(pm, "opt1"));
		assertEquals(exec(pm, "opt1"), null);
		logger.info("opt2 exec: " + exec(pm, "opt2"));
		assertEquals(exec(pm, "opt2"), 50.0);
		logger.info("opt3 exec: " + exec(pm, "opt3"));
		assertEquals(exec(pm, "opt3"), null);
		logger.info("opt4 exec: " + exec(pm, "opt4"));
		assertEquals(exec(pm, "opt4"), 70.0);
		logger.info("alt exec: " + eval(alt));
		assertEquals(eval(alt), 50.0);

		setValues(pm, val("x", 300.0), val("y", 200.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 510.0);

		setValues(pm, val("x", 10.0), val("y", 20.0), val("x2", 40.0),
				val("y2", 50.0), val("x3", 50.0), val("y3", 60.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 70.0);
	}

	@Test
	public void invokerLoopTest() throws Exception {

		EntryModel pm = entModel("prc-model");
		add(pm, prc("x", 1));
		add(pm, prc("y", invoker("x + 1", args("x"))));
		add(pm, prc("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(exec(pm, "iloop"), 48);

	}

	@Test
	public void incrementorBy1Test() throws Exception {
		EntryModel pm = entModel("prc-model");
		add(pm, prc("x", 1));
		add(pm, prc("y", invoker("x + 1", args("x"))));
		add(pm, prc("z", inc(invoker(pm, "y"))));

		for (int i = 0; i < 10; i++) {
			logger.info("" + eval(pm, "z"));
		}
		assertTrue(exec(pm, "z").equals(13));
	}

	@Test
	public void incrementorBy2Test() throws Exception {
		EntryModel pm = entModel("prc-model");
		add(pm, prc("x", 1));
		add(pm, prc("y", invoker("x + 1", args("x"))));
		add(pm, prc("z", inc(invoker(pm, "y"), 2)));

		for (int i = 0; i < 10; i++) {
			logger.info("" + eval(pm, "z"));
		}
		assertEquals(exec(pm, "z"), 24);
	}
}
