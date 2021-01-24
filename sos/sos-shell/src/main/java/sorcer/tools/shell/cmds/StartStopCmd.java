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

package sorcer.tools.shell.cmds;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import net.jini.admin.Administrable;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.LookupDiscovery;
import sorcer.tools.shell.ServiceShell;
import sorcer.tools.shell.ShellCmd;

import com.sun.jini.admin.DestroyAdmin;
import sorcer.tools.shell.WhitespaceTokenizer;

public class StartStopCmd extends ShellCmd {
	{
		COMMAND_NAME = "start, stop";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "start <application key> or stop <registrar index> | all";

		COMMAND_HELP = "Start application. " 
			+ "\nStop a single lookup service or all lookup services.";
		
	}

	private PrintStream out;

	private String input;

	public StartStopCmd() {
	}

	public void execute(String... args) throws Throwable {
		out = ServiceShell.getShellOutputStream();
		LookupDiscovery ld = ServiceShell.getDisco();
		WhitespaceTokenizer myTk = ServiceShell.getShellTokenizer();
		input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");
		if (input.startsWith("start")) {
			String app = myTk.nextToken();
			if (ServiceShell.getAppMap().containsKey(app)) {
				String path = ServiceShell.getAppMap().get(app);
				ServiceShell.startApplication(path);
			} else {
				out.print("No such application " + app);
			}
		} else {
			// pass in a clone of list - command may modify it
			ArrayList<ServiceRegistrar> registrars = new ArrayList<ServiceRegistrar>(ServiceShell.getRegistrars());
			String nxtToken;
			if (myTk.hasMoreTokens()) {
				nxtToken = myTk.nextToken();
				if (nxtToken.equals("all")) {
					out.println("  Shutting down all lookup services now");
					Iterator it = registrars.iterator();
					while (it.hasNext()) {
						ServiceRegistrar myReg = (ServiceRegistrar) it.next();
						shutdown(myReg, registrars.indexOf(myReg));
						ld.discard(myReg);
					}
				} else {
					int myIdx = Integer.parseInt(nxtToken);
					if (myIdx < registrars.size()) {
						ServiceRegistrar myReg = (ServiceRegistrar) registrars
								.get(myIdx);

						if (myReg != null) {
							shutdown(myReg, myIdx);
							ld.discard(myReg);
						}
					}
				}
			}
		}
	}

	public String getUsage(String subCmd) {
		if (subCmd.equals("start")) {
			return "start <application key>";
		} else if (subCmd.equals("stop")) {
			return "stop <lookup service index> | all";
		} else {
			return COMMAND_USAGE;
		}
	}
	
	private void shutdown(ServiceRegistrar registrar, int Idx) {
		try {
			if (registrar instanceof Administrable) {
				Administrable admin = (Administrable) registrar;
				DestroyAdmin destroyAdmin = (DestroyAdmin) admin.getAdmin();

				out.println("  Shutting down lookup service # " + Idx + " now!");
				destroyAdmin.destroy();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
