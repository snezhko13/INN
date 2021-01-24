package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.context.Connector;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Strategy.Flow;
import sorcer.service.modeling.Model;
import sorcer.service.ContextDomain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.asis;
import static sorcer.eo.operator.*;
import static sorcer.co.operator.get;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.invoker;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * Created by Mike Sobolewski on 4/15/15.
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class RequestModels {

    private final static Logger logger = LoggerFactory.getLogger(RequestModels.class);

    @Test
    public void lambdaInvoker() throws Exception {

        Model mo = model(ent("x", 10.0), ent("y", 20.0),
                prc(invoker("fxn", cxt -> (double) value(cxt, "x")
                        + (double) value(cxt, "y")
                        + 30, args("x", "y"))));
        logger.info("invoke eval: " + eval(mo, "fxn"));
        assertEquals(exec(mo, "fxn"), 60.0);
    }

    @Test
    public void lambdaInvokerWithScope() throws Exception {

        Context scope = context(val("x1", 20.0), val("y1", 40.0));

        Model mdl = model(ent("x", 10.0), ent("y", 20.0),
            prc(invoker("fxn", (cxt) -> {
                    return (double) value(cxt, "x")
                        + (double) value(cxt, "y")
                        + (double) value(cxt, "y1")
                        + 30;
                },
                scope, args("x", "y"))));
//        logger.info("invoke eval: " + eval(mo, "fxn"));
        assertEquals(exec(mdl, "fxn"), 100.0);
        assertEquals(exec(ent("fxn", mdl)), 100.0);
        assertEquals(result(ent("fxn", mdl)), ent("fxn", 100.0));
    }

    @Test
    public void evalauteLocalAddereModel() throws Exception {

        // three entry model
        Model mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
                ent(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
                response("add", "arg/x1", "arg/x2"));

        Context out = response(mod);
        assertTrue(get(out, "add").equals(100.0));

        assertTrue(get(mod, "result/y").equals(100.0));

    }

    @Test
    public void evalauteMultiFidelityModel() throws Exception {

        // three entry model
        Model mod = model(inVal("arg/x1", 10.0), inVal("arg/x2", 90.0),
                ent("meFi", sigFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                response("meFi", "arg/x1", "arg/x2"));

        logger.info("fidelity: " + asis(mod, "meFi"));

        Context out = response(mod, fi("add", "meFi"));
//        logger.info("out: " + out);
        assertTrue(value(out, "meFi").equals(100.00));
        out = response(mod, fi("multiply", "meFi"));
//        logger.info("out: " + out);
        assertTrue(value(out, "meFi").equals(900.0));

//        logger.info("result/y:" + eval(mod, "result/y"));
        assertTrue(value(eval(mod, "result/y"), "result/y").equals(900.0));
    }

    @Test
    public void evalauteLocalAddereModel2() throws Exception {

        // three entry model
        Model mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
                ent(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
                response("add", "arg/x1", "arg/x2"));

        Context out = response(mod);
        assertTrue(get(out, "add").equals(100.0));

        assertTrue(get(mod, "result/y").equals(100.0));

    }

    @Test
    public void exertRemoteAddereModel() throws Exception {

        // three entry model
        Model mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
                ent(sig("add", Adder.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
                response("add", "arg/x1", "arg/x2"));

        Model model = exert(mod);
//        logger.info("model: " + exert(mod));
        assertTrue(get(mod, "result/y").equals(100.0));
    }

    @Test
    public void evalauteRemoteAddereModel() throws Exception {

        // three entry model
        Model mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
                ent(sig("add", Adder.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
                response("add", "arg/x1", "arg/x2"));

        Context out = response(mod);
        assertTrue(get(out, "add").equals(100.0));

        assertTrue(get(mod, "result/y").equals(100.0));

    }

    @Test
    public void exertServiceModel() throws Exception {

        // getValue a context from a subject provider
        // exerting a model with the subject provider as its service context

        Model m = model(sig("add", AdderImpl.class),
                inVal("arg/x1", 1.0), inVal("arg/x2", 2.0),
                inVal("arg/x3", 3.0), inVal("arg/x4", 4.0), inVal("arg/x5", 5.0));

        add(m, ent("add", invoker("x1 + x3", args("x1", "x3"))));

        add(m, ent("multiply", invoker("x4 * x5", args("x4", "x5"))));

        // two response paths declared
        responseUp(m, "add", "multiply");
        // exert the model
        ContextDomain model = exert(m);
        logger.info("model: " + model);

        assertTrue(response(model, "add").equals(4.0));
        System.out.println("responses: " + response(model));

        assertTrue(response(model).equals(context(ent("add", 4.0), ent("multiply", 20.0))));
//                context(prc("add", 4.0), prc("multiply", 20.0), prc("result/eval", 3.0))));

    }

    @Test
    public void serviceResponses() throws Exception {
        // getValue response from a service model

        Model m = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                aka("y1", "multiply/x1"),
                response("subtract"));

//        logger.info("response: " + response(m));
        Context out = response(m);

        assertTrue(get(out, "subtract").equals(400.0));

    }

    @Test
    public void serviceResponses2() throws Exception {
        // getValue response from a service model

        Model m = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("out", "subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                response("out"));

//        logger.info("response: " + response(m));
        Context out = response(m);

        assertTrue(get(out, "out").equals(400.0));

    }

    @Test
    public void evaluateServiceModel() throws Exception {

        // getValue response from a service model

        Model mdl = reqModel(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                aka("y1", "multiply/x1"));


        logger.info("DEPS: " + printDeps(mdl));
        // getValue a scalar response
        responseUp(mdl, "subtract");
        logger.info("response: " + response(mdl));
        Context out = response(mdl);

        assertTrue(get(out, "subtract").equals(400.0));

        // stepup a response context
//        responseUp(mdl, "add", "multiply", "y1");
        responseUp(mdl, "add", "multiply");
        out = response(mdl);
        logger.info("out: " + out);
        assertTrue(get(out, "add").equals(100.0));
        assertTrue(get(out, "multiply").equals(500.0));
        assertTrue(get(out, "subtract").equals(400.0));

//        assertTrue(response(out, "y1").equals(10.0));

        logger.info("model: " + mdl);
    }

    @Test
    public void exertModelToTaskMogram() throws Exception {

        // output connector from model to exertion
        Context outConnector = outConn(inVal("y1", "add"), inVal("y2", "multiply"), inVal("y3", "subtract"));

        Model model = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/response",
                        inPaths("multiply/out", "add/out")))),
                aka("y1", "multiply/x1"), aka("y2", "add/x2"), aka("y3", "subtract/response"));

//                dep("subtract", paths("multiply", "add")));
        logger.info("DEPS: " + printDeps(model));
        responseUp(model, "add", "multiply", "subtract");
        // specify how model connects to exertion
        outConn(model, outConnector);

//        Context out = response(model);
//        logger.info("out: " + out);

        Block block = block("mogram",
                model,
                task(sig("average", AveragerImpl.class,
                        result("average/response", inPaths("y1", "y2", "y3")))));

//        logger.info("block context: " + block.getContext());
        Context result = context(exert(block));
//        logger.info("result: " + result);

        assertTrue(value(result, "y1").equals(100.0));
        assertTrue(value(result, "y2").equals(500.0));
        assertTrue(value(result, "y3").equals(400.0));
        assertTrue(value(result, "average/response").equals(333.3333333333333));
    }

    @Test
    public void exertExertionToModelMogram() throws Exception {

        // usage of in and out connectors associated with model
        Task t4 = task(
                "t4",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                        outVal("multiply/result/y")));

        Task t5 = task(
                "t5",
                sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                        outVal("add/result/y")));

        // in connector from exertion to model
        Connector taskOutConnector = outConn(inVal("add/x1", "j2/t4/multiply/result/y"),
                inVal("multiply/x1", "j2/t5/add/result/y"));

        Job j2 = job("j2", sig("exert", ServiceJobber.class),
                t4, t5, strategy(Flow.PAR),
                taskOutConnector);

        // out connector from model
        Connector modelOutConnector = outConn(inVal("y1", "add"), inVal("y2", "multiply"), inVal("y3", "subtract"));

        Model model = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/out",
                        inPaths("multiply/out", "add/out")))));

        responseUp(model, "add", "multiply", "subtract");
        // specify how model connects to exertion
        outConn(model, modelOutConnector);

        Block block = block("mogram", j2, model);

        Context result = context(exert(block));

        logger.info("result: " + result);

        assertTrue(value(result, "add").equals(580.0));
        assertTrue(value(result, "multiply").equals(5000.0));
        assertTrue(value(result, "y1").equals(580.0));
        assertTrue(value(result, "y2").equals(5000.0));
        assertTrue(value(result, "y3").equals(4420.0));
    }

}
