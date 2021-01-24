/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CommonClassLoader implements {@link sorcer.provider.core.jsb.ComponentLoader} 
 * interface and is created by the 
 * {@link sorcer.provider.boot.RioServiceDescriptor} or 
 * {@link sorcer.provider.boot.RioActivatableServiceDescriptor} when starting a Rio 
 * service and contains common declared platform JARs to be made available to its 
 * children.
 * 
 * <p>The CommonClassLoader enables a platform oriented framework conducive towards 
 * creating a layered product. The resulting capabilities allow the declaration of 
 * JARs that are added to the CommonClassLoader, making the classes accessible by 
 * all ClassLoader instances which delegate to the CommonClassLoader. In this 
 * fashion a platform can be declared, initialized and made available.
 * 
 * <p>The ClassLoader hierarchy when starting a Rio service is as follows :
 * <br>
<dataTable cellpadding="2" cellspacing="2" border="0"
 style="text-align: left; width: 50%;">
  <tbody>
    <tr>
      <td style="vertical-align: top;">
        <pre>
                  AppCL
                    |
            CommonClassLoader (http:// URLs of common JARs)
                   |
            +-------+-------+----...---+
            |               |          |
        Service-1CL   Service-2CL  Service-nCL
        </pre>
      </td>
    </tr>
  </tbody>
</dataTable>
 <span style="font-weight: bold;">AppCL</span> - Contains the main()
class of the container. Main-Class in
manifest points to <span style="font-family: monospace;">com.sun.jini.start.ServiceStarter</span><br>
Classpath:&nbsp; boot.jar, start.jar, jsk-platform.jar<br>
Codebase: none<br>
<br>
<span style="font-weight: bold;">CommonClassLoader</span> - Contains
the common Rio and
Jini technology classes (and other declared common platform JARs) to be
made available to its children.<br>
Classpath: Common JARs such as rio.jar<br>
Codebase: Context dependent. The codebase returned is the codebase of
the specific child CL that is the current context of the request.<br>
<br>
<span style="font-weight: bold;">Service-nCL</span> - Contains the
service specific implementation classes.<br>
Classpath: serviceImpl.jar<br>
Codebase: "serviceX-dl.jar rio-api.jar jsk-lib-dl.jar"<br>

 @author Dennis Reedy
 */
public class CommonClassLoader extends URLClassLoader {
    private static final String COMPONENT = "sorcer.provider.boot";
    private static Logger logger = LoggerFactory.getLogger(COMPONENT);
    private static final Map<String, URL[]> components = new HashMap<String, URL[]>();
    private static ArrayList<URL> codebaseComponents = new ArrayList<URL>();
    private static CommonClassLoader instance;

	/**
	 * Create a CommonClassLoader
     *
     * @param parent The parent ClassLoader
	 */
    private CommonClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }
    
    /**
     * Get an instance of the CommonCLassLoader
     * 
     * @return The CommonClassLoader
     */
    public static synchronized CommonClassLoader getInstance() {
        if(instance==null) {
            instance = new CommonClassLoader(ClassLoader.getSystemClassLoader());
        }
        return(instance);
    }    
    
    /**
     * Override getURLs to ensure when an Object is marshalled its
     * annotation is correct
     * 
     * @return Array of URLs
     */
    public URL[] getURLs() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL[] urls = doGetURLs(cl);
        if(logger.isTraceEnabled()) {
            StringBuffer buffer = new StringBuffer();
            for(int i=0; i<urls.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(urls[i].toExternalForm());
            }
            logger.trace(
                       "Context ClassLoader={0} URLs={1}",
                       new Object[] {cl.toString(),
                                     buffer.toString()
            });
        } 
        return(urls);
    }

    /**
     * Get the URLs, ensuring when an Object is marshalled the annotation is
     * correct
     *
     * @param cl The current context ClassLoader
     *
     * @return Array of URLs
     */
    private URL[] doGetURLs(ClassLoader cl) {
        URL[] urls;
        if(cl.equals(this)) {
            urls = super.getURLs();
        } else {
            if(cl instanceof ServiceClassLoader) {
                ServiceClassLoader scl = (ServiceClassLoader)cl;
                urls = scl.getURLs();
            } else {
                urls = super.getURLs();
            }
        }
        return(urls);
    }

    /**
	 * @see sorcer.provider.core.jsb.ComponentLoader#testComponentExistence
	 */
	public boolean testComponentExistence(String name) {
		boolean exists = false;
		/* First check if the class is registered in the component Map */
		synchronized(components) {
			if(components.containsKey(name))
				exists = true;
		}		
		if(!exists) {					
			/* Although not registered, it may be in our search path, 
			 * so try and load the class */
			try {
				loadClass(name);
				exists = true;
			} catch(Throwable t) {
				if(logger.isTraceEnabled())
					logger.trace("Failed to find class "+name);
			}
		}        
		return(exists);
	}
    
    /**
     * Add common JARs
     * 
     * @param jars Array of URLs
     */
    public void addCommonJARs(URL[] jars) {
        if(jars==null)
            return;
        for (URL jar : jars) {
            if (!hasURL(jar))
                addURL(jar);
        }
    }
	
    /**
     * Add common codebase JARs
     * 
     * @param jars Array of URLs
     */
    void addCodebaseComponents(URL[] jars) {
        if(jars==null)
            return;
        for (URL jar : jars) {
            if (!codebaseComponents.contains(jar))
                codebaseComponents.add(jar);
        }
        addCommonJARs(jars);
    }
	/**
	 * @see sorcer.provider.core.jsb.ComponentLoader#testResourceExistence
	 */
	public boolean testResourceExistence(String name) {
		return getResource(name) != null;
	}

	/**
	 * @see sorcer.provider.core.jsb.ComponentLoader#addComponent
	 */
	public void addComponent(String name, URL[] urls) {
		boolean added = false;
		boolean toAdd = false;
        boolean toReplace = false;
		synchronized (components) {
			if(components.containsKey(name)) {
			    /* Check if codebase matches */
                URL[] fetched = components.get(name);
                if(fetched.length==urls.length) {                    
                    for(int i=0; i<fetched.length; i++) {
                        /* There is a difference, replace */
                        if(!fetched[i].equals(urls[i])) {
                            toReplace = true;
                            break;
                        }
                    }
                } else {
                    /* Since the codebase is different, replace the entry */
                    toReplace = true;
                }
				
			} else {
                /* Not found, add the entry */
                toAdd = true;
			}
            
			if(toAdd || toReplace) {
                added = true;
                if(logger.isTraceEnabled()) {
                    String action = (toAdd?"Adding":"Replacing");
                    logger.trace(action+" Component "+name);
                }
                components.put(name, urls);
            } else {
                if(logger.isTraceEnabled()) {
                    StringBuffer buffer = new StringBuffer();
                    URL[] codebase = components.get(name);
                    for(int i=0; i<codebase.length; i++) {
                        if(i>0)
                            buffer.append(":");
                        buffer.append(codebase[i].toExternalForm());
                    }
                    logger.trace(
                               "Component "+name+" has "+
                               "already been registered with a "+
                               "codebase of "+buffer.toString());
                }
            }
		}
		if(added) {
            for (URL url : urls) {
                if (!hasURL(url)) {
                    addURL(url);
                }
            }
		}        
	}

	/**
	 * @see sorcer.provider.core.jsb.ComponentLoader#load
	 */
	public Object load(String name) throws 
	ClassNotFoundException, IllegalAccessException, InstantiationException {		
	    if (name == null)
	        throw new NullPointerException("key is null");
	    boolean registered;
	    synchronized (components) {
	        registered = components.containsKey(name);			
	    }		
	    if(!registered) {
	        if(testComponentExistence(name)) {
	            if(logger.isTraceEnabled())
	                logger.trace("Loading unregistered component "+name);
	        } else { 
	            throw new ClassNotFoundException ("Unregistered component "+name);
	        }
	    }			
	    Class component = loadClass(name);				
	    return(component.newInstance());
	}
    
    /*
     * Check if the URL already is registered
     */
    private boolean hasURL(URL url) {
        URL[] urls = getURLs();
        for (URL url1 : urls) {
            if (url1.equals(url))
                return (true);
        }
        return(false);
    }

    /**
     * Returns a string representation of this class loader.
     **/
    public String toString() {
        URL[] urls = doGetURLs(this);
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<urls.length; i++) {
            if(i>0)
                buffer.append(" ");
            buffer.append(urls[i].toExternalForm());
        }
        return(super.toString()+" ["+buffer.toString()+"]");
    }    
} 
