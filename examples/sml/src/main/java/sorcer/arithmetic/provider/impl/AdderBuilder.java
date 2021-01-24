package sorcer.arithmetic.provider.impl;

import sorcer.core.context.model.ent.EntryModel;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;

import java.rmi.RemoteException;

import static sorcer.eo.operator.args;
import static sorcer.ent.operator.*;
import static sorcer.mo.operator.*;

/**
 * @author Mike Sobolewski
 *
 */
public class AdderBuilder {

	@SuppressWarnings("rawtypes")
	public static EntryModel getAdderModel() throws EvaluationException,
			RemoteException, ContextException {

		EntryModel pm = entModel("prc-model");
		add(pm, prc("x", 10.0), prc("y", 20.0));
		add(pm, invoker("add", "x + y", args("x", "y")));
		return pm;
	}
}
