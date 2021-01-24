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

package sorcer.mo;

import sorcer.co.tuple.ExecDependency;
import sorcer.co.tuple.InoutValue;
import sorcer.co.tuple.InputValue;
import sorcer.co.tuple.OutputValue;
import sorcer.core.context.*;
import sorcer.core.context.model.Transmodel;
import sorcer.core.context.model.req.AnalysisModel;
import sorcer.core.plexus.ContextFidelityManager;
import sorcer.core.service.SignatureDomain;
import sorcer.service.Analysis;
import sorcer.core.context.model.DataContext;
import sorcer.core.context.model.ent.EntryModel;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.req.Req;
import sorcer.core.context.model.req.RequestModel;
import sorcer.core.dispatch.ProvisionManager;
import sorcer.core.dispatch.SortingException;
import sorcer.core.dispatch.SrvModelAutoDeps;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MorphFidelity;
import sorcer.core.service.Collaboration;
import sorcer.core.service.Governance;
import sorcer.service.Morpher;
import sorcer.service.*;
import sorcer.service.ContextDomain;
import sorcer.service.modeling.*;
import sorcer.service.Region;
import sorcer.util.DataTable;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.ent.operator.*;


/**
 * Created by Mike Sobolewski on 4/26/15.
 */
public class operator {

    protected static int count = 0;

    public static ServiceFidelity mdlFi(Service... models) {
        ServiceFidelity fi = new ServiceFidelity(models);
        fi.fiType = ServiceFidelity.Type.MODEL;
        return fi;
    }

    public static ServiceFidelity mdlFi(String fiName, Service... models) {
        ServiceFidelity fi = new ServiceFidelity(fiName, models);
        fi.fiType = ServiceFidelity.Type.MODEL;
        return fi;
    }

    public static <T> T putValue(Context<T> context, String path, T value) throws ContextException {
        context.putValue(path, value);
        ((ServiceMogram) context).setChanged(true);
        return value;
    }

    public static Context setValues(Context model, Context context) throws ContextException {
        ServiceContext cxt = (ServiceContext) context;
        String path;
        Object obj;
        Object v;
        Iterator i = cxt.keyIterator();
        while (i.hasNext()) {
            path = (String) i.next();
            obj = cxt.get(path);
            v = model.get(path);
            if (v instanceof Entry) {
                try {
                    ((Entry) v).setValue(obj);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            } else {
                model.putValue(path, obj);
            }
            ((ServiceContext) model).setChanged(true);
        }
        return model;
    }

    public static FreeMogram mog(String name, Arg... args) {
        return new FreeMogram(name);
    }

    public static <T> T value(Context<T> context, Arg... args)
        throws ContextException {
        try {
            synchronized (context) {
                return (T) ((ServiceContext) context).getValue(args);
            }
        } catch (Exception e) {
            throw new ContextException(e);
        }
    }

    public static Object value(Context context, String path, String domain) throws ContextException {
        if (((ServiceContext) context).getType().equals(Functionality.Type.MADO)) {
            return ((Mogram)context.getChild(domain)).getEvaluatedValue(path);
        } else {
            try {
                return ((Context) context.getChild(domain)).getValue(path);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
    }

    public static <T> T v(Context<T> context, String path,
                          Arg... args) throws ContextException {
        return value(context, path, args);
    }

    public static <T> T ev(Context<T> context, String path) throws ContextException {
        return entValue(context, path);
    }

    public static Object value(Request request, String path,
                               Arg... args) throws ContextException {
        if (request instanceof Governance) {
            return ((Governance) request).getOutput().get(path);
        }
        return null;
    }

    public static Object value(Response response, String path,
                               Arg... args) throws ContextException {
        if (response instanceof DataTable) {
            try {
                return response.getValue(path, args);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        return null;
    }

    public static <T> T entValue(Context<T> context, String path,
                              Arg... args) throws ContextException {
        Object ent = value(context, path);
        if (ent instanceof  Entry) {
            return (T) ((Entry)ent).getValue();
        } else {
            return (T) ent;
        }
    }

    public static <T> T value(Context<T> context, String path,
                              Arg... args) throws ContextException {
        try {
            T out = null;
            Object obj = context.get(path);
            if (obj != null) {
                out = (T) obj;
                if (obj instanceof Number || obj instanceof Number || obj instanceof Boolean
                    || obj.getClass().isArray() || obj instanceof Collection) {
                    out = (T) obj;
                } else if (obj instanceof Valuation) {
                    out = (T) ((Valuation) obj).valuate(args);
                } else if (obj instanceof Prc) {
                    out = (T) ((Prc) obj).evaluate(args);
                } else if (SdbUtil.isSosURL(obj)) {
                    out = (T) ((URL) obj).getContent();
                } else if (obj instanceof Entry) {
                    out = (T) context.getValue(path, args);
                }
            } else {
//                if (((ServiceContext) context).getType().equals(Functionality.Type.TRANS)) {
                String domainPath = null;
                String domain = null;
                int ind = path.indexOf("$");
                // allow $ at te end
                if (ind > 0 && path.length() > ind + 1) {
                    domainPath = path.substring(0, ind);
                    domain = path.substring(ind + 1);
                    if (context.get(domain) != null) {
                        Object val = ((ServiceContext) context.get(domain)).get(domainPath);
                        if (val instanceof Value) {
                            return (T) ((Value) val).getOut();
                        } else {
                            return (T) val;
                        }
                    }
                }

                if (((ServiceContext) context).getType().equals(Functionality.Type.MADO)) {
                    out = (T) context.getEvaluatedValue(path);
                } else if (context instanceof Model && context.getDomainStrategy().getOutcome() != null) {
                    context.getDomainStrategy().getOutcome().putValue(path, out);
                } else {
                    if (obj instanceof Getter) {
                        out = (T) ((Getter) obj).getValue(args);
                    }
                    // linked contexts and other special case of ServiceContext
                    if (out == null) {
                        out = (T) context.getValue(path, args);
                    }
                }
            }
            return out;
        } catch (MogramException | IOException e) {
            throw new ContextException(e);
        }
    }

    public static ContextDomain setValues(ContextDomain model, Entry... entries) throws ContextException {
        for (Entry e : entries) {
            Object v = model.asis(e.getName());
            Object nv = e.asis();
            String en = e.getName();
            try {
                if (v instanceof Setter) {
                    ((Setter) v).setValue(nv);
                } else if (SdbUtil.isSosURL(v)) {
                    SdbUtil.update((URL) v, e.asis());
                } else {
                    ((Context) model).putValue(e.getName(), e.asis());
                }
            } catch (RemoteException | MogramException | SignatureException re) {
                throw new ContextException(re);
            }
            ((ServiceContext) model).setChanged(true);
        }
        return model;
    }

    public static ContextDomain setValue(ContextDomain model, String entName, Object value)
        throws ContextException {
        try {
            Object entry = model.get(entName);
            if (entry == null) {
                if (model.getClass().equals(ServiceContext.class)) {
                    ((ServiceContext)model).put(entName, value);
                } else {
                    model.add(sorcer.ent.operator.ent(entName, value));
                }
            } else if (entry instanceof Entry) {
                ((Entry) entry).setValue(value);
            } else if (entry instanceof Setter) {
                ((Setter) entry).setValue(value);
            } else if (entry instanceof Prc) {
                Prc call = (Prc) entry;
                if (call.getScope() != null)
                    call.getScope().putValue(call.getName(), value);
            } else{
                ((ServiceContext) model).put(entName, value);
            }
        } catch (RemoteException e) {
            throw new ContextException(e);
        }

        ((ServiceMogram) model).setChanged(true);
        return model;
    }

    public static Model setValue(Model model, String entName, String path, Object value)
        throws ContextException {
        Object entry = model.asis(entName);
        if (entry instanceof Setup) {
            ((Setup) entry).setEntry(path, value);
        } else {
            throw new ContextException("A Setup is required with: " + path);
        }
        return model;
    }

    public static Model setValue(Model model, String entName, Function... entries)
        throws ContextException {
        Object entry = model.asis(entName);
        if (entry != null) {
            if (entry instanceof Setup) {
                for (Function e : entries) {
                    ((Setup) entry).getContext().putValue(e.getName(), e.getValue());
                }
            }
            ((Setup) entry).isValid(false);
//            ((Setup)entry).getEvaluation().setValueIsCurrent(false);
        }
        return model;
    }

    public static Model setValue(Model model, Slot... entries) throws ContextException {
        for (Slot slot : entries) {
            setValue(model, slot.getName(), slot.getValue());
        }
        return model;
    }

    public static Model setValue(Model model, Entry... entries) throws ContextException {
        for (Entry ent : entries) {
            setValue(model, ent.getName(), ent.getValue());
        }
        return model;
    }

    public static EntryModel entModel(String name, Signature builder) throws SignatureException {
        EntryModel model = (EntryModel) instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static RequestModel reqModel(String name, Signature builder) throws SignatureException {
        RequestModel model = (RequestModel) instance(builder);
        model.setBuilder(builder);
        return model;
    }

    public static EntryModel entModel(String name, Identifiable... objects)
        throws ContextException, RemoteException {
        EntryModel entModel = new EntryModel(objects);
        entModel.setName(name);
        return entModel;
    }

    public static EntryModel entModel(Identifiable... objects)
        throws ContextException, RemoteException {
        return new EntryModel(objects);
    }

    public static EntryModel entModel(Object... entries)
        throws ContextException {
        if (entries != null && entries.length == 1 && entries[0] instanceof Context) {
            ((Context) entries[0]).setModeling(true);
            try {
                return new EntryModel((Context) entries[0]);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        EntryModel model = new EntryModel();
        Object[] dest = new Object[entries.length + 1];
        System.arraycopy(entries, 0, dest, 1, entries.length);
        dest[0] = model;
        return (EntryModel) context(dest);
    }

    public static Model inConn(Model model, Context inConnector) {
        ((ServiceContext) model).getDomainStrategy().setInConnector(inConnector);
        if (inConnector instanceof Connector)
            ((Connector) inConnector).direction = Connector.Direction.IN;
        return model;
    }

    public static Model outConn(Model model, Context outConnector) {
        ((ServiceContext) model).getDomainStrategy().setOutConnector(outConnector);
        if (outConnector instanceof Connector)
            ((Connector) outConnector).direction = Connector.Direction.OUT;
        return model;
    }

    public static Model responseClear(Model model) throws ContextException {
        ((ServiceContext) model).getDomainStrategy().getResponsePaths().clear();
        return model;
    }

    public static Mogram responseUp(Mogram mogram, String... responsePaths) throws ContextException {
        if (responsePaths == null || responsePaths.length == 0) {
            ((ServiceContext) mogram).getDomainStrategy().getResponsePaths().clear();
            ((ServiceContext) mogram).getDomainStrategy().getResponsePaths().addAll(((ServiceContext) mogram).getOutPaths());
        } else {
            for (String path : responsePaths) {
                ((ServiceContext) mogram).getDomainStrategy().getResponsePaths().add(new Path(path));
            }
        }
        return mogram;
    }

    public static ContextDomain clearResponse(ContextDomain model) throws ContextException {
        ((ServiceContext) model).getDomainStrategy().getResponsePaths().clear();
        return model;
    }

    public static Mogram responseDown(Mogram mogram, String... responsePaths) throws ContextException {
        if (responsePaths == null || responsePaths.length == 0) {
            ((ServiceContext) mogram).getDomainStrategy().getResponsePaths().clear();
        } else {
            for (String path : responsePaths) {
                ((ServiceContext) mogram).getDomainStrategy().getResponsePaths().remove(new Path(path));
            }
        }
        return mogram;
    }

    public static Entry result(Entry entry) throws ContextException {
        Entry out = null;

        if (entry.asis() instanceof ServiceContext) {
            out = new Entry(entry.getName(), ((ServiceContext) entry.asis()).getValue(entry.getName()));
            return out;
        } else {
            out = new Entry(entry.getName(), entry.getImpl());
        }
        return out;
    }

    public static ServiceContext result(Mogram mogram) throws ContextException {
        if (mogram instanceof ContextDomain) {
            return (ServiceContext) ((ServiceContext) mogram).getDomainStrategy().getOutcome();
        } else if (mogram instanceof Routine) {
            return (ServiceContext) mogram.getContext();
        }
        return null;
    }

    public static Context result(Region region) throws ServiceException {
        return region.getOutput();
    }

    public static Object result(Mogram mogram, String path) throws ContextException {
        if (mogram instanceof ContextDomain) {
            return ((ServiceContext) mogram).getDomainStrategy().getOutcome().asis(path);
        } else if (mogram instanceof Routine) {
            try {
                return mogram.getContext().getValue(path);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        return null;
    }

    public static Object get(ContextDomain model, String path) {
        return model.get(path);
    }

    public static void subIn(Context target, Context context, String... varNames) throws ContextException {
        ((ServiceContext)target).substituteInputs(context, varNames);
    }

    public static ServiceContext substitute(ServiceContext model, Function... entries) throws ContextException {
        model.substitute(entries);
        return model;
    }

    public static Context substitute(Context target, Context update) throws ContextException {
        ((ServiceContext)target).substitute(update);
        return target;
    }

    public static Context ins(ContextDomain model) throws ContextException {
        return inputs(model);
    }

    public static Context allInputs(ContextDomain model) throws ContextException {
        try {
            return model.getAllInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context inputs(ContextDomain model) throws ContextException {
        try {
            return model.getInputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Context outs(ContextDomain model) throws ContextException {
        return outputs(model);
    }

    public static Context outputs(ContextDomain model) throws ContextException {
        try {
            return model.getOutputs();
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Mogram setResponse(Mogram mogram, Path... mogramPaths) throws ContextException {
        List<Path> paths = Arrays.asList(mogramPaths);
        mogram.getDomainStrategy().setResponsePaths(paths);
        return mogram;
    }

    public static Mogram setResponse(Mogram mogram, String... mogramPaths) throws ContextException {
        List<Path> paths = new ArrayList();
        for (String ps : mogramPaths) {
            paths.add(new Path(ps));
        }
        mogram.getDomainStrategy().setResponsePaths(paths);
        return mogram;
    }

    public static void init(ContextDomain model, Arg... args) throws ContextException {
        // initialize a model
        Map<String, List<ExecDependency>> depMap = ((ModelStrategy) model.getDomainStrategy()).getDependentPaths();
        Paths paths = Arg.selectPaths(args);
        if (paths != null) {
            model.getDependers().add(new ExecDependency(paths));
        }
        if (depMap != null && model instanceof Model) {
            model.execDependencies("_init_", args);
        }
    }

    public static Mogram clear(Mogram mogram) throws MogramException {
        mogram.clear();
        return mogram;
    }

    public static ServiceContext out(Contextion contextion) throws ContextException {
        if (contextion instanceof Region) {
            return (ServiceContext) contextion.getOutput();
        } else if (contextion instanceof Governance) {
            return (ServiceContext) ((Governance) contextion).getOutput();
        } else {
            return (ServiceContext) contextion.getDomainStrategy().getOutcome();
        }
    }

    public static ServiceContext in(Contextion contextion) throws ContextException {
        if (contextion instanceof Region) {
            return (ServiceContext) ((ServiceRegion) contextion).getInput();
        } else if (contextion instanceof Collaboration) {
            return (ServiceContext) ((Collaboration) contextion).getInput();
        } else if (contextion instanceof Governance) {
            return (ServiceContext) ((Governance) contextion).getInput();
        } else {
            return ((ServiceMogram) contextion).getDataContext();
        }
    }

    public static Context out(Collaboration collab, String domain) throws ContextException {
        return collab.getOutputs().select(domain);
    }

    public static Context in(Collaboration collab, Context in) throws ContextException {
        return collab.setInput(in);
    }

    public static void traced(Mogram mogram, boolean isTraced) throws ContextException {
        ((FidelityManager) mogram.getFidelityManager()).setTraced(isTraced);
    }

    public static Connector inConn(List<Entry> entries) throws ContextException {
        Connector map = new Connector();
        map.direction = Connector.Direction.IN;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Connector inConn(boolean isRedundant, Value... entries) throws ContextException {
        Connector map = new Connector();
        map.direction = Connector.Direction.IN;
        map.isRedundant = isRedundant;
        List<Entry> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
    }

    public static Connector inConn(Value... entries) throws ContextException {
        return inConn(false, entries);
    }

    public static Connector outConn(List<Entry> entries) throws ContextException {
        Connector map = new Connector();
        map.direction = Connector.Direction.OUT;
        sorcer.eo.operator.populteContext(map, entries);
        return map;
    }

    public static Connector outConn(Entry... entries) throws ContextException {
        Connector map = new Connector();
        map.direction = Connector.Direction.OUT;
        List<Entry> items = Arrays.asList(entries);
        sorcer.eo.operator.populteContext(map, items);
        return map;
    }

    public static Context.Return requestPath(String path) {
        return new Context.Return<>(path);
    }

    public static Paradigmatic modeling(Paradigmatic paradigm) {
        paradigm.setModeling(true);
        return paradigm;
    }

    public static Paradigmatic modeling(Paradigmatic paradigm, boolean modeling) {
        paradigm.setModeling(modeling);
        return paradigm;
    }

    public static Mogram addFidelities(Mogram mogram, Fidelity... fidelities) {
        for (Fidelity fi : fidelities) {
            ((FidelityManager) mogram.getFidelityManager()).put(fi.getName(), fi);
        }
        return mogram;
    }

    public static Mogram setMorpher(Mogram mogram, Morpher mdlMorpher) {
        ((MultiFiSlot) mogram).setMorpher(mdlMorpher);
        return mogram;
    }

    public static Mogram reconfigure(Mogram mogram, Fidelity... fidelities) throws ConfigurationException {
        FidelityList fis = new FidelityList();
        List<String> metaFis = new ArrayList<>();
        for (Fidelity fi : fidelities) {
            if (fi instanceof Metafidelity) {
                metaFis.add(fi.getName());
            } else if (fi instanceof Fidelity) {
                fis.add(fi);
            }
        }
        if (metaFis.size() > 0) {
            try {
                ((FidelityManager) mogram.getFidelityManager()).morph(metaFis);
            } catch (EvaluationException e) {
                throw new ConfigurationException(e);
            }
        }
        if (fis.size() > 0) {
            ((FidelityManager) mogram.getFidelityManager()).reconfigure(fis);
        }
        return mogram;
    }

    public static Mogram reconfigure(Mogram model, List fiList) throws ConfigurationException {
        if (fiList instanceof FidelityList) {
            ((FidelityManager) model.getFidelityManager()).reconfigure((FidelityList) fiList);
        } else {
            throw new ConfigurationException("A list of fidelities is required for reconfigurartion");
        }
        return model;
    }

    public static Mogram morph(Mogram model, String... fiNames) throws ConfigurationException {
//        ((FidelityManager)model.getFidelityManager()).morph(fiNames);
        try {
            model.morph(fiNames);
        } catch (ContextException | RemoteException e) {
            throw new ConfigurationException(e);
        }
        return model;
    }

    public static FreeMogram model(String name) {
        return new FreeMogram(name, Functionality.Type.MODEL);
    }

    public static FreeMogram routine(String name) {
        return new FreeMogram(name, Functionality.Type.ROUTINE);
    }

    public static FreeContextion pipeline(String name) {
        return new FreeContextion(name, Functionality.Type.PIPELINE);
    }

    public static Model model(Object... items) throws ContextException {
        String name = "unknown" + count++;
        boolean hasEntry = false;
        boolean aneType = false;
        boolean procType = false;
        boolean srvType = false;
        boolean hasExertion = false;
        boolean hasSignature = false;
        boolean isFidelity = false;
        Fidelity responsePaths = null;
        boolean autoDeps = true;
        for (Object i : items) {
            if (i instanceof String) {
                name = (String) i;
            } else if (i instanceof Routine) {
                hasExertion = true;
            } else if (i instanceof Signature) {
                hasSignature = true;
            } else if (i instanceof Entry) {
                try {
                    hasEntry = true;
                    if (i instanceof Prc)
                        procType = true;
                    else if (i instanceof Req || i instanceof Snr) {
                        srvType = true;
                    }
                } catch (Exception e) {
                    throw new ModelException(e);
                }
            } else if (i.equals(Strategy.Flow.EXPLICIT)) {
                autoDeps = false;
            } else if (i instanceof Fidelity) {
                if (((Fidelity) i).getFiType() == Fi.Type.RESPONSE) {
                    responsePaths = (Fidelity<Path>) i;
                }
            }
        }

        if ((hasEntry || hasSignature && hasEntry) && !hasExertion) {
            Model mo = null;
            if (srvType) {
                mo = reqModel(items);
            } else if (procType) {
                if (isFidelity) {
                    mo = reqModel(entModel(items));
                } else {
                    mo = entModel(items);
                }
            }
            // default model
            if (mo == null) {
                mo = entModel(items);
            }
            mo.setName(name);
            if (mo instanceof RequestModel && autoDeps) {
                try {
                    mo = new SrvModelAutoDeps((RequestModel) mo).get();
                } catch (SortingException e) {
                    throw new ContextException(e);
                }
            }
            if (responsePaths != null) {
                mo.getDomainStrategy().setResponsePaths(responsePaths.getSelects());
            }
            ((ModelStrategy) mo.getDomainStrategy()).setOutcome(new ServiceContext(name + "-Output)"));
            return mo;
        }
        throw new ModelException("do not know what model to create");
    }

    public static Transmodel tModel(Object... data) throws ContextException {
        String name = getUnknown();
        List<Domain> domains = new ArrayList<>();
        List<ServiceFidelity> modelFis = new ArrayList<>();
        Dependency dependency = null;
        ExecDeps execDeps = null;
        Paths domainPaths = null;

        List<Object> dataList = new ArrayList<>();
        for (Object o : data) {
            dataList.add(o);
        }
        for (int i = 0; i < dataList.size(); i++) {
            Object o = dataList.get(i);
            if (o instanceof String) {
                name = (String) o;
            } else if (o instanceof Domain) {
                domains.add((Domain) o);
            } else if (o instanceof ServiceFidelity) {
                modelFis.add((ServiceFidelity) o);
            } else if (o instanceof ExecDeps) {
                execDeps = (ExecDeps) o;
            } else if (o instanceof Paths) {
//                && ((Paths)o).type.equals(Functionality.Type.TRANS)) {
                domainPaths = (Paths) o;
            } else if (o instanceof Dependency && ((Dependency) o).getDependencyType() == Function.Type.TRANS) {
                dependency = (Dependency) o;
            }
        }
        dataList.remove(name);
        for (Object mod : domains) {
            dataList.remove(mod);
        }
        for (Object fi : modelFis) {
            dataList.remove(fi);
        }
        Transmodel transModel = new AnalysisModel(name);
        try {
            transModel.addChildren(domains);
            Object[] names = new Object[domains.size()];
            for (int i = 0; i < domains.size(); i++) {
                ((ServiceMogram) domains.get(i)).setParent(transModel);
                names[i] = domains.get(i).getName();
            }

            if (modelFis.size() > 0) {
                FidelityManager fiManager = new FidelityManager(transModel);
                Map<String, ServiceFidelity> fis = new HashMap<>();
                for (ServiceFidelity mdlFi : modelFis) {
                    fis.put(mdlFi.getName(), mdlFi);
                    transModel.getChildren().put(mdlFi.getName(), (RequestModel) mdlFi.getSelect());
                }
                fiManager.setFidelities(fis);
                ((RequestModel)transModel).setFidelityManager(fiManager);
            }
            if (domainPaths != null) {
                domainPaths.name = transModel.getName();
                transModel.setChildrenPaths(domainPaths);
            }

            if (dependency == null && names.length > 0) {
                if (domainPaths != null) {
                    sorcer.co.operator.dependsOn(transModel, ent(transModel.getName(), domainPaths));
                } else {
                    sorcer.co.operator.dependsOn(transModel, ent(transModel.getName(), paths(names)));
                }
            } else {
                List<Evaluation> entries = dependency.getDependers();
                for (Evaluation e : entries) {
                    if (e instanceof Entry && ((Entry) e).getName().equals("self")) {
                        e.setName(transModel.getName());
                    }
                }
            }

            if (execDeps != null && names.length > 0) {
                ExecDependency[] entries = execDeps.deps;
                for (Evaluation e : entries) {
                    if (e instanceof Entry && ((Entry) e).getName().equals("self")) {
                        e.setName(transModel.getName());
                    }
                }

                if (execDeps.getType().equals(Functionality.Type.FUNCTION)) {
                    sorcer.co.operator.dependsOn(transModel, execDeps.deps);
                } else if (execDeps.getType().equals(Functionality.Type.DOMAIN)) {
                    sorcer.co.operator.dependsOn(transModel, execDeps.deps);
                }
            }

            Object[] dest = new Object[dataList.size() + 1];
            dest[0] = transModel;
            for (int i = 0; i < dataList.size(); i++) {
                dest[i + 1] = dataList.get(i);
            }
            reqModel(dest);
        } catch (ContextException | SignatureException e) {
            throw new EvaluationException(e);
        }
        return transModel;
    }

    public static Context add(ContextDomain model, Identifiable... objects)
        throws ContextException, RemoteException {
        return add((Context) model, objects);
    }

    public static Context add(Context context, Identifiable... objects)
        throws RemoteException, ContextException {
        if (context instanceof Model) {
            return (Context) context.add(objects);
        }
        boolean isReactive = false;
        for (Identifiable i : objects) {
            if (i instanceof Reactive && ((Reactive) i).isReactive()) {
                isReactive = true;
            }
            if (i instanceof Mogram) {
                ((Mogram) i).setScope(context);
                i = req(i);
            }
            if (context instanceof PositionalContext) {
                PositionalContext pc = (PositionalContext) context;
                if (i instanceof InputValue) {
                    if (isReactive) {
                        pc.putInValueAt(i.getName(), i, pc.getTally() + 1);
                    } else {
                        pc.putInValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                    }
                } else if (i instanceof OutputValue) {
                    if (isReactive) {
                        pc.putOutValueAt(i.getName(), i, pc.getTally() + 1);
                    } else {
                        pc.putOutValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                    }
                } else if (i instanceof InoutValue) {
                    if (isReactive) {
                        pc.putInoutValueAt(i.getName(), i, pc.getTally() + 1);
                    } else {
                        pc.putInoutValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                    }
                } else {
                    if (i instanceof Value) {
                        pc.putValueAt(i.getName(), ((Entry) i).getOut(), pc.getTally() + 1);
                    } else {
                        if (context instanceof EntryModel || isReactive) {
                            pc.putValueAt(i.getName(), i, pc.getTally() + 1);
                        } else {
                            pc.putValueAt(i.getName(), ((Entry) i).getImpl(), pc.getTally() + 1);
                        }
                    }
                }
            } else if (context instanceof ServiceContext) {
                if (i instanceof InputValue) {
                    if (i instanceof Reactive) {
                        context.putInValue(i.getName(), i);
                    } else {
                        context.putInValue(i.getName(), ((Function) i).getImpl());
                    }
                } else if (i instanceof OutputValue) {
                    if (isReactive) {
                        context.putOutValue(i.getName(), i);
                    } else {
                        context.putOutValue(i.getName(), ((Function) i).getImpl());
                    }
                } else if (i instanceof InoutValue) {
                    if (isReactive) {
                        context.putInoutValue(i.getName(), i);
                    } else {
                        context.putInoutValue(i.getName(), ((Function) i).getImpl());
                    }
                } else {
                    if (context instanceof EntryModel || isReactive) {
                        context.putValue(i.getName(), i);
                    } else {
                        context.putValue(i.getName(), ((Entry) i).getImpl());
                    }
                }
            }

            if (i instanceof Entry) {
                Entry e = (Entry) i;
                if (e.getAnnotation() != null) {
                    context.mark(e.getName(), e.annotation().toString());
                }
                if (e.asis() instanceof Scopable) {
                    if (e.asis() instanceof ServiceInvoker) {
                        ((ServiceInvoker) e.asis()).setInvokeContext(context);
                    } else {
                        ((Scopable) e.asis()).setScope(context);
                    }
                }
            }
        }
        context.isChanged();
        return context;
    }

    public static Model aneModel(String name, Object... objects)
        throws ContextException, RemoteException {
        return reqModel(name, objects);
    }

    public static EntryModel entModel(String name, Object... objects)
        throws RemoteException, ContextException {
        EntryModel pm = new EntryModel(name);
        for (Object o : objects) {
            if (o instanceof Identifiable)
                pm.add((Identifiable) o);
        }
        return pm;
    }

    public static Object get(EntryModel pm, String parname, Arg... parametrs)
        throws ContextException, RemoteException {
        Object obj = pm.asis(parname);
        if (obj instanceof Prc)
            obj = ((Prc) obj).evaluate(parametrs);
        return obj;
    }

    public static Model reqModel(Object... items) throws ContextException {
        sorcer.eo.operator.Complement complement = null;
        Fidelity<Path> responsePaths = null;
        RequestModel model = null;
        FidelityManager fiManager = null;
        List<Metafidelity> metaFis = new ArrayList<>();
        List<Req> morphFiEnts = new ArrayList();
        List<Fidelity> fis = new ArrayList<>();
        Projection inPathPrj = null;
        Projection outPathPrj = null;
        List<Projection> cxtPrjs = new ArrayList<>();
        Context fiContext = null;
        for (Object item : items) {
            if (item instanceof sorcer.eo.operator.Complement) {
                complement = (sorcer.eo.operator.Complement) item;
            } else if (item instanceof Model) {
                model = ((RequestModel) item);
            } else if (item instanceof FidelityManager) {
                fiManager = ((FidelityManager) item);
            } else if (item instanceof Context && ((ServiceContext) item).getType().equals(Functionality.Type.MFI_CONTEXT)) {
                fiContext = (Context) item;
            } else if (item instanceof Projection) {
                if (((Projection) item).getFiType().equals(Fi.Type.IN_PATH)) {
                    inPathPrj = (Projection) item;
                } else if (((Projection) item).getFiType().equals(Fi.Type.OUT_PATH)) {
                    outPathPrj = (Projection) item;
                } else if (((Projection) item).getFiType().equals(Fi.Type.CXT_PRJ)) {
                    cxtPrjs.add((Projection) item);
                }
            } else if (item instanceof Req && ((Req) item).getImpl() instanceof MorphFidelity) {
                morphFiEnts.add((Req) item);
            } else if (item instanceof Fidelity) {
                if (item instanceof Metafidelity) {
                    metaFis.add((Metafidelity) item);
                } else {
                    if (((Fidelity) item).getFiType() == Fi.Type.RESPONSE) {
                        responsePaths = (Fidelity<Path>) item;
                    }
                }
            } else if (item instanceof Entry && ((Entry) item).getMultiFi() != null) {
                Fidelity fi = (Fidelity) ((Entry) item).getMultiFi();
                fi.setName(((Entry) item).getName());
                fi.setPath(((Entry) item).getName());
                fis.add(fi);
            }
        }

        boolean newModel = false;
        if (model == null) {
            model = new RequestModel();
            newModel = true;
        }

        if (inPathPrj != null) {
            model.setInPathProjection(inPathPrj);
        }
        if (outPathPrj != null) {
            model.setOutPathProjection(outPathPrj);
        }
        ContextFidelityManager cxtMgr = null;
        if (cxtPrjs.size() > 0) {
            cxtMgr = new ContextFidelityManager(model);
            Map<String, Fidelity> fiMap = new HashMap();
            for (Projection p : cxtPrjs) {
                fiMap.put(p.getName(), p);
            }
            cxtMgr.setFidelities(fiMap);
            if (fiContext != null) {
                cxtMgr.setDataContext(fiContext);
            }
            model.append((Context) fiContext.getMultiFi().getSelect());
            model.setContextFidelityManager(cxtMgr);
            model.setContextProjection(cxtPrjs.get(0));
        }

        if (morphFiEnts.size() > 0 || metaFis.size() > 0 || fis.size() > 0) {
            if (fiManager == null)
                fiManager = new FidelityManager(model);
        }
        if (fiManager != null) {
            fiManager.setMogram(model);
            model.setFidelityManager(fiManager);
            fiManager.init(metaFis);
            fiManager.add(fis);
            MorphFidelity mFi = null;
            if ((morphFiEnts.size() > 0)) {
                for (Req morphFiEnt : morphFiEnts) {
                    mFi = (MorphFidelity) morphFiEnt.getImpl();
                    fiManager.addMorphedFidelity(morphFiEnt.getName(), mFi);
                    fiManager.addFidelity(morphFiEnt.getName(), mFi.getFidelity());
                    mFi.setPath(morphFiEnt.getName());
                    mFi.setSelect(mFi.getSelects().get(0));
                    mFi.addObserver(fiManager);
                    if (mFi.getMorpherFidelity() != null) {
                        // set the default morpher
                        mFi.setMorpher((Morpher) ((Entry) mFi.getMorpherFidelity().get(0)).getImpl());
                    }
                }
            }
        }

        if (responsePaths != null) {
            model.getDomainStrategy().setResponsePaths(responsePaths.getSelects());
        }
        if (complement != null) {
            model.setSubject(complement.getName(), complement.getId());
        }

        if (newModel) {
            Object[] dest = new Object[items.length + 1];
            System.arraycopy(items, 0, dest, 1, items.length);
            dest[0] = model;
            return (Model) domainContext(dest);
        }
        return (Model) domainContext(items);
    }

    public static void update(Mogram mogram, Setup... entries) throws ContextException {
        try {
            mogram.update(entries);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static void run(sorcer.util.Runner runner, Arg... args) throws SignatureException, MogramException {
        runner.exec(args);
    }

    public static String printDeps(Mogram model) throws SortingException, ContextException {
        return new SrvModelAutoDeps((RequestModel) model).printDeps();
    }

    public static boolean provision(Signature... signatures) throws DispatchException {
        ProvisionManager provisionManager = new ProvisionManager(Arrays.asList(signatures));
        return provisionManager.deployServices();
    }

    public static Routine[] clients(Routine... consumers) {
        return consumers;
    }

    public static Service[] servers(Service... servers) {
        return servers;
    }

    public static Region rgn(Service server, Routine consumer) {
        return new ServiceRegion(server, consumer);
    }

    public static Region rgn(Service[] servers, Routine[] clients) {
        return new ServiceRegion(servers, clients);
    }

    public static Region rgn(List<Service> servers,
                             List<Routine> clients) {
        return new ServiceRegion(servers, clients);
    }

    public static Region rgn(Governance multidisc, String name) {
        return multidisc.getDiscipline(name);
    }

    public static String rgn(Context context) {
        return (String) context.get(Functionality.Type.REGION.toString());
    }


    public static void analyze(Collaboration collab, Context context) throws ContextException {
        collab.analyze(context);
    }

    public static Context dmnCxt(Request request, String domainName) {
        if (request instanceof Context) {
            try {
                return getDomainContext((Context)request, domainName);
            } catch (ContextException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Context setDmnCxt(Request request, String domainName, Context context) throws ContextException {
        if (request instanceof Context) {
            try {
                return addDomainContext((Context)request, context, domainName);
            } catch (ContextException e) {
                throw new RuntimeException(e);
            }
        }  if (request instanceof Collaboration) {
            context.setName(domainName);
            addDomainContext(((Collaboration)request).getInput(), context);
        }
        return context;
    }

    public static Context dmnIn(Request request, String domainName) {
        if (request instanceof Collaboration) {
            return ((Collaboration) request).getChildrenContexts().get(domainName);
        } else {
            return ((Transdomain) request).getChildrenContexts().get(domainName);
        }
    }

    public static Context dmnOut(Request request, String domainName) {
        if (request instanceof Collaboration) {
            ContextList outs = ((Collaboration) request).getOutputs();
            if (outs != null) {
                return outs.select(domainName);
            }
        } else if (request instanceof Transdomain) {
            return ((Transmodel) request).getChildrenContexts().get(domainName);
        } else if (request instanceof Context) {
            return ((Map<String, Context>)((Context)request).get(Context.DOMAIN_OUTPUTS_PATH)).get(domainName);
        }
        return null;
    }

    public static void setOut(Service request, Context object) {
        if (request instanceof Collaboration) {
            ((Collaboration) request).setOutput((Context)object);
        } else if (request instanceof Slot) {
            ((Slot)request).setOut(object);
        }
    }

    public static void setDmnOut(Request request, Context context) {
        if (request instanceof Collaboration) {
            ((Collaboration) request).getOutputs().set(context);
        }
    }

    public static Domain dmn(Collaboration collab, String name) {
        return (Domain) collab.getDomain(name);
    }

    public static String dmnName(Context context) {
        return (String) context.get(Functionality.Type.DOMAIN.toString());
    }

    public static boolean isExec(Domain domain)  {
        return domain.isExec();
    }

    public static Domain notExec(Domain domain)  {
        domain.setExec(false);
        return domain;
    }
    public static Domain setExec(Domain domain)  {
        domain.setExec(true);
        return domain;
    }

    public static Domain subject(Context context) {
        Object subject = context.getSubjectValue();
        if (subject instanceof Domain) {
            return (Domain)subject;
        }
        return null;
    }

    public static Model model(Context context) {
        Object subject = context.getSubjectValue();
        if (subject instanceof Model) {
            return (Model)subject;
        }
        return null;
    }

    public static Collaboration target(Context context) {
        Object subject = context.getSubjectValue();
        if (subject instanceof Collaboration) {
            return (Collaboration)subject;
        }
        return null;
    }

    public static String clbName(Context context) {
        return (String) context.get(Functionality.Type.COLLABORATION.toString());
    }

    public static String domain(Context context) {
        return (String) context.get(Functionality.Type.DOMAIN.toString());
    }

    public static SignatureDomain domain(Signature signature) throws ContextException {
        return new SignatureDomain(signature);
    }

    public static SignatureDomain domain(String name, Signature signature) throws ContextException {
        return new SignatureDomain(name, signature);
    }

    public static Region rgn(Fidelity... discFis) {
        return rgn((String) null, discFis);
    }

    public static Region rgn(String name, Fidelity... discFis) {
        ServiceRegion srvDisc = null;
        if (discFis[0] instanceof RegionFidelity) {
            srvDisc = new ServiceRegion(((RegionFidelity) discFis[0]).getContextionFi(),
                ((RegionFidelity) discFis[0]).getDispatcherFi(),
                ((RegionFidelity) discFis[0]).getContextFi());
            srvDisc.getDisciplineFidelities().put(discFis[0].getName(), (RegionFidelity) discFis[0]);
            for (int i = 1; i < discFis.length; i++) {
                srvDisc.add(((RegionFidelity) discFis[i]).getContextionFi(),
                    ((RegionFidelity) discFis[i]).getDispatcherFi(),
                    ((RegionFidelity) discFis[i]).getContextFi());
                srvDisc.getDisciplineFidelities().put(discFis[i].getName(), (RegionFidelity) discFis[i]);
            }
        } else {
            srvDisc = new ServiceRegion(discFis[0], discFis[1]);
        }
        if (name != null) {
            srvDisc.setName(name);
        }
        return srvDisc;
    }

    public static Region add(Region disciplne, Service server, Routine client) {
        disciplne.add(server, client, null);
        return disciplne;
    }

    public static Region add(Region disciplne, Fidelity providerFi, Fidelity clientFi) {
        disciplne.add(providerFi, clientFi, null);
        return disciplne;
    }

    public static Region add(Region disciplne, Service server, Routine client, Context context) {
        disciplne.add(server, client, context);
        return disciplne;
    }

    public static Region add(Region disciplne, Fidelity providerFi, Fidelity clientFi, Fidelity contextFi) {
        disciplne.add(providerFi, clientFi, contextFi);
        return disciplne;
    }

    public static Collaboration clb(Object... data) throws ContextException {
        if (data[0] instanceof Context &&
                ((Context)data[0]).getSubjectValue() instanceof Collaboration) {
            return (Collaboration) ((Context)data[0]).getSubjectValue();
        }
        String name = getUnknown();
        List<Domain> domains = new ArrayList<>();
        List<ServiceFidelity> discFis = new ArrayList<>();
        Dependency dependency = null;
        ExecDeps execDeps = null;
        Paths domainPaths = null;
        Context collabContext = null;

        List<Object> dataList = new ArrayList<>();
        for (Object o : data) {
            dataList.add(o);
        }
        for (int i = 0; i < dataList.size(); i++) {
            Object o = dataList.get(i);
            if (o instanceof String) {
                name = (String) o;
            } else if (o instanceof Context && !(o instanceof Model || o instanceof Routine)
                && ((ServiceContext)o).getType() != Functionality.Type.EXEC) {
                collabContext = (Context) o;
            } else if (o instanceof Domain) {
                domains.add((Domain) o);
            } else if (o instanceof Dependency) {
                dependency = (Dependency) o;
            } else if (o instanceof ExecDeps) {
                execDeps = (ExecDeps) o;
            } else if (o instanceof ServiceFidelity) {
                discFis.add((ServiceFidelity) o);
            } else if (o instanceof Paths && ((Paths) o).type.equals(Functionality.Type.COLLABORATION)) {
                domainPaths = (Paths) o;
            }
        }

        Collaboration collab = new Collaboration(name, domains);
        collab.setDomainName(name);
        if (collabContext != null) {
            collab.setInput(collabContext);
        }
        Object[] names = new Object[domains.size()];

        for (int i = 0; i < domains.size(); i++) {
            domains.get(i).setParent(collab);
            names[i] = domains.get(i).getName();
        }

        if (discFis.size() > 0) {
            FidelityManager fiManager = new FidelityManager();
            Map<String, ServiceFidelity> fis = new HashMap<>();
            for (ServiceFidelity discFi : discFis) {
                fis.put(discFi.getName(), discFi);
                collab.getChildren().put(discFi.getName(), (Domain) discFi.getSelect());
            }
            fiManager.setFidelities(fis);
            collab.setFiManager(fiManager);
        }

        if (domainPaths == null) {
            domainPaths = new Paths();
            for (Domain d : domains) {
                domainPaths.add(new Path(d.getName()));
            }
        }

        domainPaths.name = collab.getName();
        collab.setDomainPaths(domainPaths);

        if (dependency == null && names.length > 0) {
            if (domainPaths != null) {
                sorcer.co.operator.dependsOn(collab, ent(collab.getName(), domainPaths));
            } else {
                sorcer.co.operator.dependsOn(collab, ent(collab.getName(), paths(names)));
            }
        } else {
            List<Evaluation> entries = dependency.getDependers();
            for (Evaluation e : entries) {
                if (e instanceof Entry && ((Entry) e).getName().equals("self")) {
                    e.setName(collab.getName());
                }
            }
        }

        if (execDeps != null && names.length > 0) {
            ExecDependency[] entries = execDeps.deps;
            for (Evaluation e : entries) {
                if (e instanceof Entry && ((Entry) e).getName().equals("self")) {
                    e.setName(collab.getName());
                }
            }

            if (execDeps.getType().equals(Functionality.Type.FUNCTION)) {
                sorcer.co.operator.dependsOn(collab, execDeps.deps);
            } else if (execDeps.getType().equals(Functionality.Type.DOMAIN)) {
                sorcer.co.operator.dependsOn(collab, execDeps.deps);
            }
        }
        try {
            collab.initializeDomains();
        } catch (SignatureException e) {
            throw new ContextException(e);
        }
//        collab.setExplorer(new Explorer(collab));
        return collab;
    }

    public static ContextList componentContexts(String name, Context... data) throws ContextException {
        ContextList contextList = new ContextList(data);
        contextList.setName(name);
        return contextList;
    }

    public static ContextList componentContexts(Context... data) throws ContextException {
        ContextList contextList = new ContextList(data);
        return contextList;
    }

    public static Governance gov(Object... data) throws ContextException {
        String name = getUnknown();
        List<Region> regions = new ArrayList<>();
        List<ServiceFidelity> discFis = new ArrayList<>();
        Dependency dependency = null;
        ExecDeps execDeps = null;
        Paths disciplinePaths = null;
        Context govContext = null;

        List<Object> dataList = new ArrayList<>();
        for (Object o : data) {
            dataList.add(o);
        }
        for (int i = 0; i < dataList.size(); i++) {
            Object o = dataList.get(i);
            if (o instanceof String) {
                name = (String) o;
            } else if (o instanceof Region) {
                regions.add((Region) o);
            } else if (o instanceof DataContext) {
                govContext = (Context) o;
            } else if (o instanceof Dependency) {
                dependency = (Dependency) o;
            } else if (o instanceof ExecDeps) {
                execDeps = (ExecDeps) o;
            } else if (o instanceof ServiceFidelity) {
                discFis.add((ServiceFidelity) o);
            } else if (o instanceof Paths && ((Paths) o).type.equals(Functionality.Type.REGION)) {
                disciplinePaths = (Paths) o;
            }
        }

        Governance gov = new Governance(name, regions);
        if (govContext != null) {
            gov.setInput(govContext);
        }
        Object[] names = new Object[regions.size()];

        for (int i = 0; i < regions.size(); i++) {
            ((ServiceRegion) regions.get(i)).setParent(gov);
            names[i] = regions.get(i).getName();
        }

        if (discFis.size() > 0) {
            FidelityManager fiManager = new FidelityManager();
            Map<String, ServiceFidelity> fis = new HashMap<>();
            for (ServiceFidelity discFi : discFis) {
                fis.put(discFi.getName(), discFi);
                gov.getDisciplines().put(discFi.getName(), (Region) discFi.getSelect());
            }
            fiManager.setFidelities(fis);
            gov.setFiManager(fiManager);
        }

        if (disciplinePaths != null) {
            disciplinePaths.name = gov.getName();
            gov.setDisciplnePaths(disciplinePaths);
        }

        if (dependency == null && names.length > 0) {
            if (disciplinePaths != null) {
                sorcer.co.operator.dependsOn(gov, ent(gov.getName(), disciplinePaths));
            } else {
                sorcer.co.operator.dependsOn(gov, ent(gov.getName(), paths(names)));
            }
        } else {
            List<Evaluation> entries = dependency.getDependers();
            for (Evaluation e : entries) {
                if (e instanceof Entry && ((Entry) e).getName().equals("self")) {
                    e.setName(gov.getName());
                }
            }
        }

        if (execDeps != null && names.length > 0) {
            ExecDependency[] entries = execDeps.deps;
            for (Evaluation e : entries) {
                if (e instanceof Entry && ((Entry) e).getName().equals("self")) {
                    e.setName(gov.getName());
                }
            }

            if (execDeps.getType().equals(Functionality.Type.FUNCTION)) {
                sorcer.co.operator.dependsOn(gov, execDeps.deps);
            } else if (execDeps.getType().equals(Functionality.Type.REGION)) {
                sorcer.co.operator.dependsOn(gov, execDeps.deps);
            }
        }
        return gov;
    }

    public static Fidelity mdaFi(String name, String path) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.MDA;
        fi.setOption(Fi.Type.SELECT);
        return fi;
    }

    public static SupervisionEntry sup(String name, Supervision supervisor)
        throws EvaluationException {
        return new SupervisionEntry(name, supervisor);
    }

    public static ServiceFidelity supFi(String name, Supervision... supEntries) {
        SupervisionEntry[] entries = new SupervisionEntry[supEntries.length];
        for (int i = 0; i < supEntries.length; i++) {
            entries[i] = (SupervisionEntry) supEntries[i];
        }
        ServiceFidelity sFi = new ServiceFidelity(entries);
        sFi.setName(name);
        sFi.setType(Fi.Type.SUP);
        return sFi;
    }

    public static Analysis mda(String name, Analysis mda)
        throws EvaluationException {
        return new AnalysisEntry(name, mda);
    }

    public static ServiceFidelity mdaFi(String name, Analysis... mdaEntries) {
        AnalysisEntry[] entries = new AnalysisEntry[mdaEntries.length];
        for (int i = 0; i < mdaEntries.length; i++) {
            entries[i] = (AnalysisEntry) mdaEntries[i];
        }
        ServiceFidelity mdaFi = new ServiceFidelity(entries);
        mdaFi.setName(name);
        mdaFi.setType(Fi.Type.MDA);
        return mdaFi;
    }

    public static ServiceFidelity explFi(String name, Exploration... explEntries) {
        ExplorationEntry[] entries = new ExplorationEntry[explEntries.length];
        for (int i = 0; i < explEntries.length; i++) {
            entries[i] = (ExplorationEntry) explEntries[i];
        }
        ServiceFidelity eFi = new ServiceFidelity(entries);
        eFi.setName(name);
        eFi.setType(Fi.Type.EXPLORER);
        return eFi;
    }

    public static Exploration expl(String name, Exploration explorer)
        throws EvaluationException {
        return new ExplorationEntry(name, explorer);
    }

    public static Exploration expl(String name, Signature signature)
        throws EvaluationException {
        ExplorationEntry ee = new ExplorationEntry(name, signature);
        ee.setType(Functionality.Type.EXPL);
        try {
            ee.setValue(signature);
        } catch (SetterException | RemoteException e) {
            throw new EvaluationException(e);
        }
        return ee;
    }

    public static Analysis mdaInstace(String name, Signature signature)
        throws EvaluationException {
        AnalysisEntry mda = new AnalysisEntry(name, signature);
        mda.setType(Functionality.Type.MDA);
        try {
            mda.setValue(signature);
            mda.setImpl(instance(signature));
        } catch (SetterException | SignatureException | RemoteException e) {
            throw new EvaluationException(e);
        }
        return mda;
    }

    public static Analysis mda(String name, Signature signature)
        throws EvaluationException {
        AnalysisEntry mda = new AnalysisEntry(name, signature);
        mda.setType(Functionality.Type.MDA);
        try {
            mda.setValue(signature);
        } catch (SetterException | RemoteException e) {
            throw new EvaluationException(e);
        }
        return mda;
    }

    public static ContextList getDomainContexts(Context context) throws ContextException {
        return (ContextList) context.get(Context.COMPONENT_CONTEXT_PATH);
    }

    public static DispatcherList getDomainDispatchers(Context context) throws ContextException {
        return (DispatcherList) context.get(Context.COMPONENT_DISPATCHER_PATH);
    }

    public static Context getDomainContext(ContextDomain context, String domain) throws ContextException {
        if (context instanceof Transdomain) {
            return ((Transdomain)context).getChildrenContexts().get(domain);
        } else if (context instanceof Context) {
            Object domainContexts = context.get(Context.COMPONENT_CONTEXT_PATH);
            if (domainContexts instanceof ContextList && ((ContextList) domainContexts).size() > 0) {
                return ((ContextList) domainContexts).select(domain);
            }
        }
        return null;
    }

    public static Dispatch getDomainDispatcher(Context context, String domain) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainDispatchers = context.get(Context.COMPONENT_DISPATCHER_PATH);
            if (domainDispatchers instanceof DispatcherList && ((DispatcherList) domainDispatchers).size() > 0) {
                return ((DispatcherList) domainDispatchers).select(domain);
            }
        }
        return null;
    }

    public static Context addDomainContext(Context context, Context domainContext, String domainName) throws ContextException {
        Context cxt = addDomainContext(context, domainContext);
        cxt.setName(domainName);
        return cxt;
    }

    public static Dispatch addDomainDispatcher(Context context, Dispatch domainDispatcher, String domainName) throws ContextException {
        Dispatch disp = addDomainDispatcher(context, domainDispatcher);
        ((Mogram) disp).setName(domainName);
        return disp;
    }

    public static Context addDomainContext(Context context, Context domainContext) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainContexts = context.get(Context.COMPONENT_CONTEXT_PATH);
            if (domainContexts == null) {
                domainContexts = new ContextList();
                ((ServiceContext) context).put(Context.COMPONENT_CONTEXT_PATH, domainContexts);
            }
            if (domainContexts instanceof ContextList) {
                ((List) domainContexts).add(domainContext);
                return domainContext;
            }
        }
        return null;
    }

    public static Dispatch addDomainDispatcher(Context context, Dispatch domainDispatcher) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainDispatchers = context.get(Context.COMPONENT_DISPATCHER_PATH);
            if (domainDispatchers == null) {
                domainDispatchers = new DispatcherList();
                ((ServiceContext) context).put(Context.COMPONENT_DISPATCHER_PATH, domainDispatchers);
            }
            if (domainDispatchers instanceof DispatcherList) {
                ((List) domainDispatchers).add(domainDispatcher);
                return domainDispatcher;
            }
        }
        return null;
    }

    public static Context updateDomainContext(Context context, Context domainContext) throws ContextException {
        if (context instanceof ServiceContext) {
            ContextList domainContexts = (ContextList) context.get(Context.COMPONENT_CONTEXT_PATH);
            Context edc = domainContexts.select(domainContext.getName());
            Iterator<Map.Entry<String, Object>> eit =
                ((ServiceContext)domainContext).getData().entrySet().iterator();
            while (eit.hasNext()) {
                Map.Entry edce = eit.next();
                ((ServiceContext)context).put((String) edce.getKey(), edce.getValue());
            }
        }
        return context;
    }

    public static Context setDomainContext(Context context, Context domainContext) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainContexts = context.get(Context.COMPONENT_CONTEXT_PATH);
            if (domainContexts == null) {
                domainContexts = new ContextList();
                ((ServiceContext) context).put(Context.COMPONENT_CONTEXT_PATH, domainContexts);
            }
            if (domainContexts instanceof ContextList) {
                return ((ContextList) domainContexts).set(domainContext);
            }
        }
        return null;
    }

    public static Dispatch setDomainDispatcher(Context context, Dispatch domainDispatcher) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainDispatchers = context.get(Context.COMPONENT_DISPATCHER_PATH);
            if (domainDispatchers == null) {
                domainDispatchers = new DispatcherList();
                ((ServiceContext) context).put(Context.COMPONENT_DISPATCHER_PATH, domainDispatchers);
            }
            if (domainDispatchers instanceof DispatcherList) {
                return ((DispatcherList) domainDispatchers).set(domainDispatcher);
            }
        }
        return null;
    }

    public static void removeDomainContext(Context context, String domain) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainContexts = context.get(Context.COMPONENT_CONTEXT_PATH);
            if (domainContexts instanceof ContextList && ((ContextList) domainContexts).size() > 0) {
                ((ContextList) domainContexts).remove(domain);
            }
        }
    }

    public static void removeDomainDispatchers(Context context, String domain) throws ContextException {
        if (context instanceof ServiceContext) {
            Object domainDispatchers = context.get(Context.COMPONENT_DISPATCHER_PATH);
            if (domainDispatchers instanceof DispatcherList && ((DispatcherList) domainDispatchers).size() > 0) {
                ((ContextList) domainDispatchers).remove(domain);
            }
        }
    }

    // for contexts as executable domains (model)
    public static Context setExec(Context cxt) {
        ((ServiceContext)cxt).setType(Functionality.Type.EXEC);
        return cxt;
    }
}
