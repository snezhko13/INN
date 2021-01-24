/*
 * Copyright to the original author or authors.
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

/*
 * Configuration for a Cybernode
 */
import net.jini.core.discovery.LookupLocator
import org.rioproject.config.Component
import org.rioproject.config.Constants
import org.rioproject.impl.client.JiniClient
/*
 * Declare Cybernode properties
 */
@Component('org.rioproject.cybernode')
class CybernodeConfig {
    String serviceName = sorcer.util.SorcerEnv.getActualName('Cybernode')
    String serviceComment = 'Dynamic Agent'
    String jmxName = 'org.rioproject.cybernode:type=Cybernode'
    boolean provisionEnabled = true
    //long provisionerLeaseDuration = 1000*60

    String[] getInitialLookupGroups() {
        //String groups = System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))
        String groups = System.getProperty(Constants.GROUPS_PROPERTY_NAME, sorcer.util.SorcerEnv.getLookupGroupsAsString())
        
        return groups.split(",")
    }

    LookupLocator[] getInitialLookupLocators() {
        String locators = System.getProperty(Constants.LOCATOR_PROPERTY_NAME)
        if(locators!=null) {
            def lookupLocators = JiniClient.parseLocators(locators)
            return lookupLocators as LookupLocator[]
        } else {
            return null
        }
    }

    String getServiceLogRootDirectory() {
        String logExt = System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))
        String serviceLogRootDirectory = System.getProperty("rio.log.dir")
        if(serviceLogRootDirectory==null) {
            String opSys = System.getProperty('os.name')
            String rootLogDir = opSys.startsWith("Windows")?System.getProperty("java.io.tmpdir"):'/tmp'
            String name = System.getProperty('user.name')
            serviceLogRootDirectory = rootLogDir+File.separator+name+File.separator+'logs'+File.separator+logExt
        }
        return serviceLogRootDirectory
    }

    String getNativeLibDirectory() {
        return System.getProperty("rio.native.dir")
    }
}
