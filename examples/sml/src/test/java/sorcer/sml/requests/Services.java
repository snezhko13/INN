package sorcer.sml.requests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Function;
import sorcer.core.context.model.ent.Value;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.evr;
import sorcer.service.modeling.func;
import sorcer.util.GenericUtil;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.ent.operator.*;
import static sorcer.ent.operator.loop;
import static sorcer.so.operator.*;
import static sorcer.service.Signature.Direction;
import static sorcer.util.exec.ExecUtils.CmdResult;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Services {
	private final static Logger logger = LoggerFactory.getLogger(Services.class);


	@Test
	public void directionalEntries() throws Exception {

        Entry x0 = ent("arg/x0", 10.0);
        assertEquals(10.0, exec(x0));
        assertTrue(direction(x0) == null);

        Function x1 = prc("arg/x1", 100.0);
        assertEquals(100.0, exec(x1));
        assertTrue(direction(x1) == null);

		Value x2 = inVal("arg/x2", 20.0);
		assertEquals(20.0, exec(x2));
        assertTrue(direction(x2) == Direction.IN);

		Entry x3 = outVal("arg/x3", 80.0);
		assertEquals(80.0, exec(x3));
        assertTrue(direction(x3) == Direction.OUT);

        // entry of entry
		Entry x4 = inoutVal("arg/x4", x3);
		assertEquals(exec(x3), exec(x4));
        assertTrue(direction(x4) == Direction.INOUT);
		assertEquals(name(impl(x4)), "arg/x3");
        assertTrue(direction(x4) == Direction.INOUT);
    }

    @Test
    public void entFidelities() throws Exception {
        Entry mfiEnt = inVal("by", entFi(inVal("by-10", 10.0), inVal("by-20", 20.0)));

        assertTrue(exec(mfiEnt, fi("by-20", "by")).equals(20.0));
        assertTrue(exec(mfiEnt, fi("by-10", "by")).equals(10.0));
    }

	@Test
	public void expressionEntry() throws Exception {

		Function z1 = prc("z1", expr("x1 + 4 * x2 + 30",
					context(prc("x1", 10.0), prc("x2", 20.0)),
                    args("x1", "x2")));

		assertEquals(120.0, exec(z1));
	}

	@Test
	public void bindingEntryArgs() throws Exception {

		Function y = prc("y", expr("x1 + x2", args("x1", "x2")));

		assertTrue(exec(y, val("x1", 10.0), val("x2", 20.0)).equals(30.0));
	}


	public static class Doer implements Invocation<Double> {

        @Override
        public Double invoke(Context<Double> cxt, Arg... args) throws RemoteException, ContextException {
            Entry<Double> x = val("x", 20.0);
            Entry<Double> y = val("y", 30.0);
            Entry<Double> z = prc("z", invoker("x - y", x, y));

            if (value(cxt, "x") != null)
                setValue(x, value(cxt, "x"));
            if (value(cxt, "y") != null)
                setValue(y, value(cxt, "y"));
            return exec(y) + exec(x) + exec(z);
        }

        public Object execute(Arg... args) throws ServiceException, RemoteException {
            return invoke(null, args);
        }

    }

    @Test
    public void methodInvokerContext() throws Exception {

        Object obj = new Doer();

        // no scope for invocation
        Entry m1 = prc("m1", methodInvoker("invoke", obj));
        assertEquals(exec(m1), 40.0);

        // method invocation with a scope
        Context scope = context(val("x", 200.0), val("y", 300.0));
        m1 = prc("m1", methodInvoker("invoke", obj, scope));
        assertEquals(exec(m1), 400.0);
    }


    @Test
	public void methodInvokerModel() throws Exception {

        Object obj = new Doer();

        // no scope for invocation
        Entry m1 = prc("m1", methodInvoker("invoke", obj));
        assertEquals(exec(m1), 40.0);

        // method invocation with a scope
        Context scope = context(val("x", 200.0), val("y", 300.0));
        m1 = prc("m1", methodInvoker("invoke", obj, scope));
        assertEquals(exec(m1), 400.0);
    }

    @Test
    public void systemCmdInvoker() throws Exception {
        Args args;
        if (GenericUtil.isLinuxOrMac()) {
            args = args("sh", "-c", "echo $USER");
        } else {
            args = args("cmd",  "/C", "echo %USERNAME%");
        }

        Function cmd = prc("cmd", invoker(args));

        CmdResult result = (CmdResult) exec(cmd);
        logger.info("result: " + result);

        logger.info("result out: " + result.getOut());
        logger.info("result err: " + result.getErr());
        if (result.getExitValue() != 0)
            throw new RuntimeException();
        String userName = (GenericUtil.isWindows() ? System.getenv("USERNAME") : System.getenv("USER"));

        logger.info("User: " + userName);
        logger.info("Got: " + result.getOut());
        assertEquals(userName.toLowerCase(), result.getOut().trim().toLowerCase());
    }

    @Test
    public void signatureEntry() throws Exception {

        Function y1 = req("y1", sig("add", AdderImpl.class, result("add/out",
                        inPaths("x1", "x2"))),
                    context(inVal("x1", 10.0), inVal("x2", 20.0)));

        assertEquals(30.0, exec(y1));
    }

    @Test
    public void getEntryValueWithArgSelector() throws Exception {

        Function y1 = req("y1", sig("add", AdderImpl.class),
                context(inVal("x1", 10.0), inVal("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1, selector("result/eval")));
        assertEquals(30.0,  exec(y1, selector("result/eval")));
    }

    @Test
    public void getEntryValueWithSelector() throws Exception {

        Function y1 = req("y1", sig("add", AdderImpl.class),
                context(inVal("x1", 10.0), inVal("x2", 20.0)),
                selector("result/eval"));

//        logger.info("out eval: {}", eval(y1));
        assertEquals(30.0,  exec(y1));
    }

	@Test
	public void getConditionalCallValueContextScope() throws Exception {

		func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
			opt(condition((Context<Double> cxt) -> value(cxt, "x1")
                    <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))),
			context(val("x1", 10.0), val("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(30.0,  exec(y1));
	}

    @Test
    public void getConditionalCallValueModelScopel() throws Exception {

        func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
                opt(condition((Context<Double> cxt) -> value(cxt, "x1")
                        <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))),
                model(prc("x1", 10.0), prc("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1));
        assertEquals(30.0,  exec(y1));
    }

	@Test
	public void getConditionalCall2Value() throws Exception {

		func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
			opt(condition((Context<Double> cxt) -> v(cxt, "x1")
                    <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))),
			model(prc("x1", 20.0), prc("x2", 10.0)));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(200.0,  exec(y1));
	}

	@Test
	public void getConditionalCall3Value() throws Exception {

		func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
			opt(30.0)),
			model(prc("x1", 10.0), prc("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(30.0,  exec(y1));
	}

	@Test
	public void getConditionalValueEntModel() throws Exception {

		Model mdl = model(
			val("x1", 10.0), val("x2", 20.0),
			prc("y1", alt(opt(condition((Context<Double> cxt)
                            -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
				opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2"))))));

//        logger.info("out eval: {}", eval(mdl, "y1"));
		assertEquals(30.0,  exec(mdl, "y1"));
	}

    @Test
    public void getConditionalBlockSrvValue() throws Exception {

        Function y1 = req("y1", block(context(prc("x1", 10.0), prc("x2", 20.0)),
            alt(opt(condition((Context<Double> cxt)
                            -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 + x2", args("x1", "x2"))),
                opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2"))))));

//        logger.info("out eval: {}", eval(y1));
        assertEquals(30.0,  exec(y1));
    }

    @Test
    public void getConditionalValueBlockSrvModel() throws Exception {

        Model mdl = model(
            val("x1", 10.0), val("x2", 20.0),
            req("y1", block(alt(opt(condition((Context<Double> cxt)
                            -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
                    opt(condition((Context<Double> cxt) -> v(cxt, "x1")
                            <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))))));

//        logger.info("out eval: {}", eval(mdl, "y1"));
        assertEquals(30.0,  exec(mdl, "y1"));
    }

	@Test
	public void conditionalFunctionalModel() throws Exception {

		Model mdl = model(
			val("x1", 10.0), val("x2", 20.0),
			req("y1", alt(opt(condition((Context<Double> cxt)
					-> v(cxt, "x1") > v(cxt, "x2")), func(expr("x1 * x2", args("x1", "x2")))),
				opt(condition((Context<Double> cxt) -> v(cxt, "x1")
					<= v(cxt, "x2")), func(expr("x1 + x2", args("x1", "x2")))))));

//        logger.info("out: {}", exec(mdl, "y1"));
        evr ev1 = (evr) exec(mdl, "y1");
//		logger.info("out: {}", eval(ev1, mdl));

		assertEquals(30.0,  eval(ev1, mdl));
		assertEquals(30.0,  exec(ev1, mdl));
	}

	@Test
	public void getConditionalLoopSrvValue() throws Exception {

		func y1 = prc("y1",
			loop(condition((Context<Double> cxt) -> v(cxt, "x1") < v(cxt, "x2")),
				invoker("fxn",
                        (Context<Double> cxt) -> { putValue(cxt, "x1", v(cxt, "x1") + 1.0);
                            return v(cxt, "x1") * v(cxt, "x3"); },
				    context(val("x1", 10.0), val("x2", 20.0), val("x3", 40.0)),
                    args("x1", "x2", "x3"))));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(800.0,  exec(y1));
	}

	@Test
	public void getConditionalLoopSrvModel() throws Exception {

		Model mdl = model(
			val("x1", 10.0), val("x2", 20.0), val("x3", 40.0),
			prc("y1",
				loop(condition((Context<Double> cxt) -> v(cxt, "x1") < v(cxt, "x2")),
					invoker("fxn",
						(Context<Double> cxt) -> { putValue(cxt, "x1", v(cxt, "x1") + 1.0);
							return v(cxt, "x1") * v(cxt, "x3"); },
                        args("x1", "x2", "x3")))));

//        logger.info("out eval: {}", eval(mdl, "y1"));
		assertEquals(800.0,  exec(mdl, "y1"));
	}

}
