/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
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

package sorcer.tools.shell;

import net.jini.config.Configuration;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.entry.Name;
import sorcer.tools.shell.cmds.DiscoCmd;

import java.rmi.RemoteException;
import java.util.ArrayList;

@SuppressWarnings("rawtypes")
abstract public class ShellCmd {

	protected String COMMAND_NAME;

	protected String NOT_LOADED_MSG;

	protected String COMMAND_USAGE;

	protected String COMMAND_HELP;

	protected ServiceShellIf shell;

	protected Configuration config;

	protected static final int MAX_MATCHES = 64;

	abstract public void execute(String... args) throws Throwable;

	public String getCommandWord() {
		return COMMAND_NAME;
	}

	public String getUsage(String subCmd) {
		return COMMAND_USAGE;
	}

	public String getShortHelp() {
		return COMMAND_HELP;
	}

	public String getLongDescription(String subCmd) {
		return COMMAND_HELP;
	}

	public String nameConflictDetected(Class<?> conflictClass) {
		return NOT_LOADED_MSG;
	}

	public void initializeSubsystem() {
	}

	public void endSubsystem() {
	}

	public void setServiceShell(ServiceShellIf shell){
		this.shell = shell;
	}

	public void setConfiguration(Configuration config){
		this.config = config;
	}

	public String toString() {
		return getClass().getName() + ": " + COMMAND_NAME;
	}

	public static ServiceItem[] lookup(
			Class[] serviceTypes) throws RemoteException {
		return lookup(serviceTypes, (String)null);
	}
	
	public static ServiceItem[] lookup(
			Class[] serviceTypes, String serviceName) throws RemoteException {
		return lookup(null, serviceTypes, serviceName);
	}

	public static ServiceItem[] lookup(ServiceRegistrar registrar,
			Class[] serviceTypes, String serviceName) throws RemoteException {
		ServiceRegistrar regie = null;
		if (registrar == null) {
			regie = DiscoCmd.getSelectedRegistrar();
			if (regie == null)
				return null;
		} else {
			regie = registrar;
		}

		ArrayList<ServiceItem> serviceItems = new ArrayList<ServiceItem>();
		ServiceMatches matches;
		Entry myAttrib[] = null;
		if (serviceName != null) {
			myAttrib = new Entry[1];
			myAttrib[0] = new Name(serviceName);
		}
		ServiceTemplate myTmpl = new ServiceTemplate(null, serviceTypes,
				myAttrib);

		matches = regie.lookup(myTmpl, MAX_MATCHES);
		for (int j = 0; j < Math.min(MAX_MATCHES, matches.totalMatches); j++) {
			serviceItems.add(matches.items[j]);
		}
		ServiceItem[] sItems = new ServiceItem[serviceItems.size()];
		return serviceItems.toArray(sItems);
	}

}
