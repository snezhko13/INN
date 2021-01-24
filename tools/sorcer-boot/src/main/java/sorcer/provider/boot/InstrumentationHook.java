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

import java.lang.instrument.Instrumentation;

/**
 * Allows the instrumentation class to be accessed
 *
 * @author Dennis Reedy
 */
public class InstrumentationHook {
    static Instrumentation inst;

    public static Instrumentation getInstrumentation() {
        return(inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationHook.inst = inst;        
    }
    
}
