/*
  GRANITE DATA SERVICES
  Copyright (C) 2009 ADEQUATE SYSTEMS SARL

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.grails.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.core.io.Resource;


public class GrailsGAEWebSWFServlet extends GrailsWebSWFServlet {
    
    private static final long serialVersionUID = 1L;
    
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        

        request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);

        // Get the name of the Groovy script (intern the name so that we can
        // lock on it)
        String pageName = "/swf"+request.getServletPath();      
        
        Resource requestedFile = getResourceForUri(pageName);
        
        File swfFile = requestedFile.getFile();
        if (requestedFile == null || swfFile==null || !swfFile.exists()) {
            response.sendError(404, "\"" + pageName + "\" not found.");
            return;
        }
        
        response.setContentType("application/x-shockwave-flash");
        response.setContentLength((int)swfFile.length());
        response.setBufferSize((int)swfFile.length());
        response.setDateHeader("Expires", 0);
        
        byte[] buf = new byte[1000000];
        FileInputStream is = null;
        try {
            is = new FileInputStream(swfFile);
            OutputStream os = response.getOutputStream();
            int read = is.read(buf, 0, 1000000);
            while (read > 0) {
            	os.write(buf, 0, read);
            	read = is.read(buf, 0, 1000000);
            }
        } 
        finally {
            if (is != null)
                is.close();
        }
    }

    /**
     * Attempts to retrieve a reference to a GSP as a Spring Resource instance for the given URI.
     *
     * @param uri The URI to check
     * @return A Resource instance
     */
    public Resource getResourceForUri(String uri) {
        Resource r = getResourceWithinContext(uri);
        if (r != null && r.exists())
            return r;
        
        // try plugin
        String pluginUri = GrailsResourceUtils.WEB_INF + uri;
        r = getResourceWithinContext(pluginUri);
        if (r != null && r.exists())
            return r;
        
        uri = getUriWithinGrailsViews(uri);
        return getResourceWithinContext(uri);
    }

    private Resource getResourceWithinContext(String uri) {
        Resource r = servletContextLoader.getResource(uri);
        if (r.exists()) 
            return r;
        return resourceLoader != null ? resourceLoader.getResource(uri) : null;
    }
    
    /**
     * Returns the path to the view of the relative URI within the Grails views directory
     *
     * @param relativeUri The relative URI
     * @return The path of the URI within the Grails view directory
     */
    protected String getUriWithinGrailsViews(String relativeUri) {
        StringBuffer buf = new StringBuffer();
        String[] tokens;
        if (relativeUri.startsWith("/"))
            relativeUri = relativeUri.substring(1);

        if (relativeUri.indexOf('/') >= 0)
            tokens = relativeUri.split("/");
        else
            tokens = new String[] { relativeUri };

        buf.append(GrailsApplicationAttributes.PATH_TO_VIEWS);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            buf.append('/').append(token);
        }
        
        return buf.toString();
    }


   @Override
    public void destroy() {
    }
}
