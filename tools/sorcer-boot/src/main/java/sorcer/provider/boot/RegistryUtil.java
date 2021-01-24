/*
 * Copyright 2008 the original author or authors.
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
package sorcer.provider.boot;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;

/**
 * Utility for getting/creating the RMI Registry.
 *
 * <h4>Configuration</h4>
 * The RegistryUtil class supports the following configuration args;
 * where each configuration entry key is associated with the component key
 * <tt>sorcer.provider.rmi</tt>.
 *
 * <ul>
 <li><span
 style="font-weight: bold; font-family: courier new,courier,monospace; color: rgb(0, 0, 0);"><a
 key="registryPort_"></a>registryPort</span>
 <dataTable style="text-align: left; width: 100%;" border="0"
 cellspacing="2" cellpadding="2">
 <tbody>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Type:<br>
 </td>
 <td style="vertical-align: top; font-family: monospace;">int</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Default:<br>
 </td>
 <td style="vertical-align: top;"><span
 style="font-family: monospace;">1099</span><br>
 </td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Description:<br>
 </td>
 <td style="vertical-align: top;">The port to use when
 creating the RMI Registry. This
 entry will be read at initialization, and based on the eval provided
 by the registryRetries property, will be incremented<br>
 </td>
 </tr>
 </tbody>
 </dataTable>
 </li>
 </ul>
 <code></code>
 <ul>
 <li><span
 style="font-weight: bold; font-family: courier new,courier,monospace; color: rgb(0, 0, 0);"><a
 key="registryRetries_"></a>registryRetries</span>
 <dataTable style="text-align: left; width: 100%;" border="0"
 cellspacing="2" cellpadding="2">
 <tbody>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Type:<br>
 </td>
 <td style="vertical-align: top; font-family: monospace;">int</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Default:<br>
 </td>
 <td style="vertical-align: top;"><span
 style="font-family: monospace;">50</span><br>
 </td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Description:<br>
 </td>
 <td style="vertical-align: top;">This
 entry will be read at initialization and provides the ability to
 recover from RMI Registry creation failure by incrementing the port the
 RMI Registry instance will accept requests on. The port provided by the
 registryPort property is used as a basis to increment from. If retries
 are needed, the registryPort is incremented by one each time until
 either the RMI Registry is created without exceptions, or the registry
 retries have been exhausted<br>
 </td>
 </tr>
 </tbody>
 </dataTable>
 </li>
 </ul>

 @author Dennis Reedy
 */
public class RegistryUtil {
    public static final int DEFAULT_PORT = 1099;
    public static final int DEFAULT_RETRY_COUNT = 50;
    static final String COMPONENT = "sorcer.provider.rmi";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);
    
    static final String BASE_COMPONENT = "sorcer.provider";
    /**
     * System property setValue when an RMI Registry is started
     */
    static final String REGISTRY_PORT = BASE_COMPONENT+".registryPort";
    
    /**
     * Check if RMI Registry has been started for the VM, if not start it.
     * This method will use the {@link sorcer.provider.config.Constants#REGISTRY_PORT}
     * system property to determine if the {@link java.rmi.registry.Registry} has
     * been created.
     *
     * <p>If the RMI Registry is created, this method will also setValue the
     * @link sorcer.provider.config.Constants#REGISTRY_PORT} system property
     *
     * @param config Configuration object to use
     *
     * @throws ConfigurationException If there are errors reading the
     * configuration
     */
    public static void checkRegistry(Configuration config) throws
                                                     ConfigurationException {
        synchronized(RegistryUtil.class) {
            if(System.getProperty(REGISTRY_PORT)==null) {
                int port = getRegistry(config);
                if(port>0)
                    System.setProperty(REGISTRY_PORT,
                                       Integer.toString(port));
            }
        }
    }

    /**
     * Check if RMI Registry has been started for the VM, if not start it.
     *
     * @param config Configuration object to use
     *
     * @return The port the RMI Registry was created on, or -1 if the
     * RMIRegistry could not be created
     * @throws ConfigurationException If there are errors reading the
     * configuration
     */
    public static int getRegistry(Configuration config) throws
                                                     ConfigurationException {
        int registryPort;
        synchronized(RegistryUtil.class) {
            registryPort = getRegistryPort(config);
            int originalPort = registryPort;
            int registryRetries = getRegistryRetries(config);
            Registry registry = null;
            for(int i = 0; i < registryRetries; i++) {
                try {
                    registry = LocateRegistry.createRegistry(registryPort);
                    break;
                } catch(RemoteException e1) {
                    if(logger.isTraceEnabled())
                        logger.trace("Failed to create RMI Registry using "+
                                      "port ["+registryPort+"], increment " +
                                      "port and try again");
                }
                registryPort++;
            }
            if(registry==null) {
                logger.warn("Unable to create RMI Registry using " +
                               "ports "+originalPort+
                               " through "+registryPort);
                registryPort = -1;
            } else {
                if(logger.isDebugEnabled())
                    logger.debug("Created RMI Registry on port="+
                                  System.getProperty(REGISTRY_PORT));
            }
        }

        return registryPort;
    }


    /**
     * Get the registryPort property.
     *
     * @param config The Configuration to use, if null a default eval is
     * returned
     *
     * @return The port used to create RMI Registry instance
     */

    public static int getRegistryPort(Configuration config) {
        int registryPort = DEFAULT_PORT;
        try {
            registryPort = (Integer) config.getEntry(COMPONENT,
                                                     "registryPort",
                                                     int.class,
                                                     DEFAULT_PORT);
        } catch(ConfigurationException e) {
            logger.warn(
                       "Reading "+COMPONENT+".registryPort",
                       e);
        }

        return(registryPort);
    }

    /**
     * Get the registryRetries property.
     *
     * @param config The Configuration to use, if null a default eval is
     * returned
     *
     * @return The number of times to attempt to find an RMI Registry instance
     */
    public static int getRegistryRetries(Configuration config) {
        int retryCount = DEFAULT_RETRY_COUNT;
        if(config==null)
            return(retryCount);
        try {
            retryCount =
                (Integer) config.getEntry(COMPONENT,
                                          "registryRetries",
                                          Integer.class,
                                          DEFAULT_RETRY_COUNT);
        } catch(ConfigurationException e) {
            logger.warn(
                       "Reading "+COMPONENT+".registryRetries",
                       e);
        }

        return(retryCount);
    }
}
