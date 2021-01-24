/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
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
package sorcer.service;

import sorcer.core.context.model.ent.Entry;

/**
 * A free entry is an instance of Entry that has a name only to be bound at runtime.
 *
 * @see Entry
 *
 * @author Mike Sobolewski
 */
public class FreeEntry extends Entry implements FreeService {

    private Entry entry;

    public FreeEntry(String name) {
        this.key = name;
    }

    @Override
    public void bind(Object object) {
        this.entry = (Entry)object;
    }
}
