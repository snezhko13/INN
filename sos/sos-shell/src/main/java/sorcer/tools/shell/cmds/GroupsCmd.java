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
import java.util.Arrays;

import net.jini.discovery.LookupDiscovery;
import sorcer.tools.shell.ServiceShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

public class GroupsCmd extends ShellCmd {

	{
		COMMAND_NAME = "groups";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "groups [-d]";

		COMMAND_HELP = "List the groups that the shell itself is in; with -d, the groups of the selected lookup.";
	}

	private PrintStream out;

	public GroupsCmd() {
	}

	public void execute(String... args) throws Throwable {
		out = ServiceShell.getShellOutputStream();
		WhitespaceTokenizer myTk = ServiceShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		if (numTokens == 0) {
			printShellGroups();
		} else if (numTokens == 1) {
			String next = myTk.nextToken();
			if (next.equals("-d")) {
				printLookupDiscoveryGroups();
			}
		} else {
			out.println(COMMAND_USAGE);
		}
	}
	
	private void printShellGroups() throws Throwable {
		String[] groups = ServiceShell.getGroups();
		if (groups == null) {
			out.println("This nsh shell groups: all groups");
		} else if (groups.length == 0) {
			out.println("This nsh shell groups: no groups");
		} else
			out.println("This nsh shell groups: " 
					+ Arrays.toString(ServiceShell.getGroups()));
	}
	
	private void printLookupDiscoveryGroups() throws Throwable {
		LookupDiscovery ld = ServiceShell.getDisco();
		String[] ldGroups = ld.getGroups();
		if (ldGroups == null) {
			out.println("Lookup discovery groups: all groups");
		} else if (ldGroups.length == 0) {
			out.println("Lookup discovery groups: no groups");
		} else {
			out.println("Lookup discovery groups: " 
					+ Arrays.toString(ldGroups));
		}
	}

}
