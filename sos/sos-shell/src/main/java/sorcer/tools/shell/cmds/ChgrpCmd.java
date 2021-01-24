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

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryGroupManagement;
import sorcer.tools.shell.ServiceShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

public class ChgrpCmd extends ShellCmd {

	{
		COMMAND_NAME = "chgrp";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "chgrp (<groups> | all) | chgrp -r <registrar index> <groups>";

		COMMAND_HELP = "Change groups (<groups> space separated) for discovery/lookup, or a selected lookup service.";
	}

	private PrintStream out;

	public ChgrpCmd() {
	}

	public void execute(String... args) throws Throwable {
		out = ServiceShell.getShellOutputStream();
		WhitespaceTokenizer myTk = ServiceShell.getShellTokenizer();
		// pass in a clone of list - command may modify it
		@SuppressWarnings("unchecked")
		ArrayList<ServiceRegistrar> registrars = new ArrayList<ServiceRegistrar>(ServiceShell
				.getRegistrars());
		int numTokens = myTk.countTokens();
		String[] groups = null; // matches all
		String next = myTk.nextToken();
		if (next.indexOf("-") != 0) {
			if (numTokens == 1) {
				if (next.indexOf(",") > 0) {
					groups = ServiceShell.toArray(next, ",");
				} else {
					groups = new String[] { next };
				}
			} else {
				String[] tg = getGroupArray(myTk, numTokens - 1);
				groups = new String[numTokens];
				groups[0] = next;
				System.arraycopy(tg, 0, groups, 1, numTokens - 1);
			}
			for (int j = 0; j < groups.length; j++) {
				if (groups[j].equalsIgnoreCase("all")) {
					groups = DiscoveryGroupManagement.ALL_GROUPS;
					break;
				}
			}
			ServiceShell.setGroups(groups);
			ServiceShell.getDisco().terminate();
			ServiceShell.getRegistrars().clear();
			DiscoCmd.selectedRegistrar = 0;
			ServiceShell.setLookupDiscovery(groups);
		} else if (next.equals("-r")) {
			int myIdx = Integer.parseInt(myTk.nextToken());
			if (numTokens == 2) {
				groups = ServiceShell.getGroups();
			} else if (numTokens > 2) {
				groups = getGroupArray(myTk, numTokens - 2);
			}
			if ((myIdx < registrars.size()) && (myIdx >= 0)) {
				ServiceRegistrar myReg = (ServiceRegistrar) registrars
						.get(myIdx);
				if (myReg != null) {
					setGroups(myReg, myIdx, groups);
				}
			}
		} else {
			out.println(COMMAND_USAGE);
		}
	}

	private String[] getGroupArray(WhitespaceTokenizer tokenizer, int size) {
		String[] groups = new String[size];
		String group;
		for (int i = 0; i < size; i++) {
			group = tokenizer.nextToken();
			if (group.equals("public")) {
				groups = new String[1];
				groups[0] = "";
				break;
			} else if (group.equals("all")) {
				groups = null;
				break;
			}
			groups[i] = group;
		}
		return groups;
	}

	private void setGroups(ServiceRegistrar registrar, int Idx, String[] gps) {
		try {
			Administrable admin = null;
			JoinAdmin jAdmin = null;
			if (registrar instanceof Administrable) {
				admin = (Administrable) registrar;
				jAdmin = (JoinAdmin) admin.getAdmin();
				out.println("\tChanging LUS groups # " + Idx + " now!");
				jAdmin.setLookupGroups(gps);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
