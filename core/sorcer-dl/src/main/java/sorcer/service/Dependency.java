/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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

import sorcer.service.modeling.Functionality;

import java.util.List;

/**
 * A functionality required by major evaluations in SORCER.
 *
 * @author Mike Sobolewski
 */
public interface Dependency {

    /**
     * Adds depeners for this dependency.
     *
     * @param dependers
     */
    public void addDependers(Evaluation... dependers);


    /**
     * Return a list of all dependent Evaluations
     * @return
     */
    public List<Evaluation> getDependers();


    /**
     * Return a functionality type
     * @return
     */
    Functionality.Type getDependencyType();

}



