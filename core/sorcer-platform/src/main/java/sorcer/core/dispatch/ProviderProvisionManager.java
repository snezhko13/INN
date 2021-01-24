/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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
package sorcer.core.dispatch;

import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.provider.Spacer;
import sorcer.core.signature.RemoteSignature;
import sorcer.ext.ProvisioningException;
import sorcer.service.*;
import sorcer.service.space.SpaceAccessor;

import static sorcer.util.StringUtils.tName;

/**
 * @author Pawel Rubach
 */
public class ProviderProvisionManager {
	private static final Logger logger = LoggerFactory.getLogger(ProviderProvisionManager.class.getName());
    private static ProviderProvisionManager instance = null;
    private static final int MAX_ATTEMPTS = 3;


    public static void provision(Routine exertion, SpaceParallelDispatcher spaceExertDispatcher) throws SignatureException {
        getInstance().doProvision(exertion, spaceExertDispatcher);
    }

    private static ProviderProvisionManager getInstance() {
        if (instance==null)
            instance = new ProviderProvisionManager();
        return instance;
    }

    private void doProvision(Routine exertion, SpaceParallelDispatcher spaceExertDispatcher) throws SignatureException {
        RemoteSignature sig = (RemoteSignature) exertion.getProcessSignature();
        // A hack to disable provisioning spacer itself
        if (!sig.getServiceType().getName().equals(Spacer.class.getName())) {
            ThreadGroup provGroup = new ThreadGroup("spacer-provisioning");
            provGroup.setDaemon(true);
            provGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);
            Thread pThread = new Thread(provGroup, new ProvisionThread(
                    new SignatureElement(sig.getServiceType().getName(), sig.getProviderName().getName(),
                            sig.getVersion(), sig, exertion, spaceExertDispatcher)
            ), tName("Provisioner-" + exertion.getName()));
            pThread.start();
        }
    }

    private class ProvisionThread implements Runnable {

        private SignatureElement srvToProvision;

        public ProvisionThread(SignatureElement sigEl) {
            this.srvToProvision = sigEl;
        }

        public void run() {
            Service service = null;
            try {
                service = (Service) Accessor.get().getService(srvToProvision.getSignature());
            } catch (SignatureException e) {
                logger.error("invalid signature" + srvToProvision);
            }
            while (service==null && srvToProvision.getProvisionAttempts() < MAX_ATTEMPTS) {
                srvToProvision.incrementProvisionAttempts();
                try {
                    logger.info("Provisioning: " + srvToProvision.getSignature());
                    service = ServiceDirectoryProvisioner.getProvisioner().provision(srvToProvision.getSignature());
                } catch (ProvisioningException pe) {
                    String msg = null;
                    try {
                        msg = "Problem provisioning " + srvToProvision.getSignature().getServiceType()
                                + " (" + srvToProvision.getSignature().getProviderName() + ")"
                                + " " + pe.getMessage();
                    } catch (SignatureException e) {
                        e.printStackTrace();
                    }
                    logger.error(msg);
                }
            }
            if (service == null ) {
                String logMsg = "Provisioning for " + srvToProvision.getServiceType() + "(" + srvToProvision.getProviderName()
                        + ") tried: " + srvToProvision.getProvisionAttempts() +" times, provisioning will not be reattempted";
                logger.error(logMsg);
                try {
                    failExertionInSpace(srvToProvision, new ProvisioningException(logMsg));
                } catch (RoutineException ile) {
                    logger.error("Problem trying to remove exertion from space after reattempting to provision");
                }
            }
        }
    }


    private void failExertionInSpace(SignatureElement sigEl, Exception exc) throws RoutineException {
        logger.info("Setting Failed state for service multitype: " + sigEl.getServiceType() + " exertion ID: " +
                "" + sigEl.getExertion().getId());
        ExertionEnvelop ee = null;
        try {
            ee = ExertionEnvelop.getTemplate(sigEl.getExertion());
        } catch (SignatureException e) {
            throw new RoutineException(e);
        }

        ExertionEnvelop result = null;
        result = sigEl.getSpaceExertDispatcher().takeEnvelop(ee);
        if (result!=null) {
            result.state = Exec.FAILED;
            ((Subroutine)result.exertion).setStatus(Exec.FAILED);
            ((Subroutine)result.exertion).reportException(exc);
            try {
                JavaSpace05 space = SpaceAccessor.getSpace();
                if (space == null) {
                    throw new RoutineException("NO exertion space available!");
                }
                space.write(result, null, Lease.FOREVER);
                logger.debug("===========================> written failure envelop: "
                        + ee.describe() + "\n to: " + space);
            } catch (Exception e) {
                //e.printStackTrace();
                logger.error(this.getClass().getName(), "failExertionInSpace", e);
                throw new RoutineException("Problem writing exertion back to space");
            }
        }
    }

    private class SignatureElement {
        String serviceType;
        String providerName;
        String version;
        Signature signature;
        int provisionAttempts = 0;
        Routine exertion;
        SpaceParallelDispatcher spaceExertDispatcher;

        private String getServiceType() {
            return serviceType;
        }

        private void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        private String getProviderName() {
            return providerName;
        }

        private void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        private String getVersion() {
            return version;
        }

        private void setVersion(String version) {
            this.version = version;
        }

        private Signature getSignature() {
            return signature;
        }

        private void setSignature(Signature signature) {
            this.signature = signature;
        }


        public int getProvisionAttempts() {
            return provisionAttempts;
        }

        public void incrementProvisionAttempts() {
            this.provisionAttempts++;
        }

        public Routine getExertion() {
            return exertion;
        }

        public SpaceParallelDispatcher getSpaceExertDispatcher() {
            return spaceExertDispatcher;
        }

        private SignatureElement(String serviceType, String providerName, String version, Signature signature,
								 Routine exertion, SpaceParallelDispatcher spaceExertDispatcher) {
            this.serviceType = serviceType;
            this.providerName = providerName;
            this.version = version;
            this.signature = signature;
            this.exertion = exertion;
            this.spaceExertDispatcher = spaceExertDispatcher;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SignatureElement that = (SignatureElement) o;
            if (!providerName.equals(that.providerName)) return false;
            if (!serviceType.equals(that.serviceType)) return false;
            if (!exertion.equals(that.exertion)) return false;
            if (version != null ? !version.equals(that.version) : that.version != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceType.hashCode();
            result = 31 * result + providerName.hashCode();
            result = 31 * result + (version != null ? version.hashCode() : 0);
            return result;
        }
    }

}
