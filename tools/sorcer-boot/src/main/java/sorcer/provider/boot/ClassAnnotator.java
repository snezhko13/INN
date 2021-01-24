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

import java.net.URI;
import java.net.URL;
import java.util.Properties;

import net.jini.loader.ClassAnnotation;

/**
 * Provides support to annotate classes required for dynamic class loading
 * in RMI
 *
 * @see net.jini.loader.ClassAnnotation
 *
 * @author Dennis Reedy
 */
public class ClassAnnotator implements ClassAnnotation {
    /**
     * URLs to return when being queried for annotations
     */
    private URL[] codebase;
    /**
     * Codebase annotation
     */
    private String exportAnnotation;
    /**
     * Optional properties the ClassAnnotator may use to control
     * behavior
     */
    private Properties properties = new Properties();

    /**
     * Constructs a new ClassAnnotator for the specified codebase URLs
     *
     * @param codebase Array of URLs to use for the codebase
     * @param props Optional properties a ClassAnnotator may use to control
     * behavior
     */
    public ClassAnnotator(URL[] codebase, Properties props) {
        if(props != null)
            properties.putAll(props);
        setAnnotationURLs(codebase);
    }

    /**
     * Get the codebase URLs used for class annotations.
     *
     * @return The codebase URLs required for class annotations
     */
    public URL[] getURLs() {
        URL[] urls = null;
        if(codebase != null) {
            urls = new URL[codebase.length];
            System.arraycopy(codebase, 0, urls, 0, urls.length);
        }
        return (urls);
    }

    /**
     * Replace the URLs used for class annotations.
     *
     * @param urls The URLs used for class annotations.
     */
    public void setAnnotationURLs(URL[] urls) {
        this.codebase = urls;
        this.exportAnnotation = urlsToPath(codebase);
    }

    /**
     * @see net.jini.loader.ClassAnnotation#getClassAnnotation
     */
    public String getClassAnnotation() {
        return (exportAnnotation);
    }

    /**
     * Get the properties
     *
     * @return A Properties object setValue at creation time. A new Properties
     * object is created each time
     */
    protected Properties getProperties() {
        return(new Properties(properties));
    }

    /**
     * Utility method that converts a <code>URL[]</code> into a corresponding,
     * space-separated string with the same array elements. Note that if the
     * array has zero elements, the return eval is the empty string.
     *
     * @param urls An array of URLs that are to be converted
     *
     * @return A space-separated string of each URL provided
     */
    public static String urlsToPath(URL[] urls) {
        if(urls.length == 0) {
            return ("");
        } else if(urls.length == 1) {
            return (urls[0].toExternalForm());
        } else {
            StringBuffer path = new StringBuffer(urls[0].toExternalForm());
            for(int i = 1; i < urls.length; i++) {
                path.append(' ');
                path.append(urls[i].toExternalForm());
            }
            return (path.toString());
        }
    }

    /**
     * Utility method that converts a <code>URI[]</code> into a corresponding,
     * space-separated string with the same array elements. Note that if the
     * array has zero elements, the return eval is the empty string.
     *
     * @param uris An array of URIs that are to be converted
     *
     * @return A space-separated string of each URI provided
     */
    public static String urisToPath(URI[] uris) {
        if(uris.length == 0) {
            return ("");
        } else if(uris.length == 1) {
            return (uris[0].toString());
        } else {
            StringBuffer path = new StringBuffer(uris[0].toString());
            for(int i = 1; i < uris.length; i++) {
                path.append(' ');
                path.append(uris[i].toString());
            }
            return (path.toString());
        }
    }

}
