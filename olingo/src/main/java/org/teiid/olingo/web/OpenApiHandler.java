/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teiid.olingo.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.core.serializer.xml.ODataXmlSerializer;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.vdb.runtime.VDBKey;

public class OpenApiHandler {
    
    private static final String OPENAPI_V2_JSON = "/swagger.json"; //$NON-NLS-1$
    //private static final String OPENAPI_V3_JSON = "/openapi.json"; //$NON-NLS-1$
    
    public static final String SCHEME = "scheme"; //$NON-NLS-1$
    public static final String HOST = "host"; //$NON-NLS-1$
    public static final String BASEPATH = "basePath"; //$NON-NLS-1$
    public static final String TITLE = "info-title"; //$NON-NLS-1$
    public static final String DESCRIPTION = "info-description"; //$NON-NLS-1$
    public static final String VERSION = "info-version"; //$NON-NLS-1$
    
    private ServletContext servletContext;
    private Map<List<?>, File> cachedMetadata = new ConcurrentHashMap<>();
    private Templates templates;
    
    public OpenApiHandler(ServletContext servletContext) throws ServletException {
        this.servletContext = servletContext;
        //initialize the template only once - it's quite expensive. can be moved to something static
        //for aot compilation
        try (InputStream template = getClass()
                .getResourceAsStream("/V4-CSDL-to-OpenAPI.xsl");) { //$NON-NLS-1$
            templates = TransformerFactory.newInstance().newTemplates(new StreamSource(template));
        } catch (TransformerConfigurationException
                | TransformerFactoryConfigurationError | IOException e) {
            throw new ServletException(e);
        }
    }
    
    public boolean isOpenApiMetadataRequest(String method, String uri) {
        return method.equalsIgnoreCase("GET") && uri.endsWith(OPENAPI_V2_JSON); //$NON-NLS-1$
    }
    
    /**
     * 
     * @param httpRequest
     * @param key
     * @param uri
     * @param modelName
     * @param response
     * @param serviceMetadata
     * @param parameters optional overrides for template parameters 
     * @throws TeiidProcessingException
     */
    public void processOpenApiMetadata(HttpServletRequest httpRequest, VDBKey key, String uri,
            String modelName, ServletResponse response, ServiceMetadata serviceMetadata, Map<String, String> parameters)
            throws TeiidProcessingException {
        try {
            List<? extends Object> cacheKey = Arrays.asList(key, modelName);
            File f = cachedMetadata.get(cacheKey);
            if (f == null) {
                Transformer transformer = templates.newTransformer();
                transformer.setParameter(SCHEME, httpRequest.getScheme()); 
                transformer.setParameter(HOST, httpRequest.getServerName()+":"+httpRequest.getServerPort()); //$NON-NLS-1$ 
                transformer.setParameter(BASEPATH, uri.substring(0, uri.length() - OPENAPI_V2_JSON.length())); 
                transformer.setParameter(TITLE, key.getName() + " - " + modelName); //$NON-NLS-1$ 
                //could also pull from the vdb description
                transformer.setParameter(DESCRIPTION, modelName); 
                transformer.setParameter(VERSION, key.getVersion()); 
                if (parameters != null) { 
                    parameters.forEach((k,v)->transformer.setParameter(k, v));
                }
                File appTempDir = (File)servletContext.getAttribute(ServletContext.TEMPDIR);
                f = File.createTempFile(key + modelName, ".json", appTempDir); //$NON-NLS-1$
                f.deleteOnExit();

                ODataXmlSerializer oxs = new ODataXmlSerializer();
                try (FileOutputStream outputStream = new FileOutputStream(f);
                        InputStream is = oxs.metadataDocument(serviceMetadata)
                                .getContent();) {
                    transformer.transform(new StreamSource(is),
                            new StreamResult(outputStream));
                }
                cachedMetadata.put(cacheKey, f);
            }

            response.setContentType("application/json"); //$NON-NLS-1$
            ObjectConverterUtil.write(response.getOutputStream(), new FileInputStream(f), -1);
            response.flushBuffer();
        } catch (ODataLibraryException
                | TransformerFactoryConfigurationError | TransformerException
                | IOException e) {
            throw new TeiidProcessingException(e);
        }

    }

}
