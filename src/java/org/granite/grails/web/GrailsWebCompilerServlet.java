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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.granite.webcompiler.WebCompiler;
import org.granite.webcompiler.WebCompilerException;
import org.granite.webcompiler.WebCompilerType;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.WebApplicationContextUtils;


public class GrailsWebCompilerServlet extends HttpServlet {
    
    private Logger log = Logger.getLogger(GrailsWebCompilerServlet.class);

    private static final long serialVersionUID = 1L;


    private ServletConfig servletConfig = null;
    private ResourceLoader resourceLoader = null;
    private ResourceLoader servletContextLoader = null;
    
    private WebCompiler webCompiler;

    private GrailsApplicationAttributes grailsAttributes;
    
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
        this.servletContextLoader = new ServletContextResourceLoader(servletConfig.getServletContext());
        ApplicationContext springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletConfig.getServletContext());
		if (springContext.containsBean(GroovyPageResourceLoader.BEAN_ID))
			this.resourceLoader = (ResourceLoader)springContext.getBean(GroovyPageResourceLoader.BEAN_ID);
		
        webCompiler = WebCompiler.getInstance();
        webCompiler.setTargetPlayer("9.0.28");
        
        try {
            webCompiler.init(servletConfig.getServletContext().getRealPath("/WEB-INF"));
        } 
        catch (Exception e) {
            log.error(e);
        }

        this.grailsAttributes = new DefaultGrailsApplicationAttributes(servletConfig.getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        
        WebCompilerType type = WebCompilerType.application;
        
        request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);

        // Get the name of the Groovy script (intern the name so that we can
        // lock on it)
        String pageName = request.getServletPath();        
        pageName = pageName.replaceAll("\\.swf$", ".mxml").intern();

        Resource requestedFile = getResourceForUri(pageName);
        if (requestedFile == null) {
            // log("\"" + pageName + "\" not found");
            response.sendError(404, "\"" + pageName + "\" not found.");
            return;
        }
        
        String requestedPath = requestedFile.getFile().getAbsolutePath();
        File mxmlFile = requestedFile.getFile();
        File sourcePath = mxmlFile.getParentFile();
        long sourcesLastModified = lastModified(sourcePath, new String[] { ".mxml", ".as" });
        
        String swfPath = requestedPath.substring(0, (requestedPath.length() - 5));
        File swfFolder = new File(swfPath.substring(0, swfPath.lastIndexOf(File.separator)) + File.separator + "swf");
        if (!swfFolder.exists())
        	swfFolder.mkdir();
        File swfFile = new File(swfFolder, swfPath.substring(swfPath.lastIndexOf(File.separator)+1) + ".swf");
		long lastModified = swfFile.exists() ? swfFile.lastModified() : 0;
        
        try {
            if (mxmlFile != null && swfFile != null && lastModified < sourcesLastModified) {
				// log("Compiling mxml file: " + mxmlFile.getAbsolutePath() + " to " + swfFile.getAbsolutePath());
                swfFile = webCompiler.compileMxmlFile(mxmlFile, swfFile, false, type, servletConfig.getServletContext().getContextPath());
			}
        }
        catch (WebCompilerException e) {
			if (swfFile == null || !swfFile.exists() || swfFile.lastModified() <= lastModified) {
				PrintWriter writer = response.getWriter();
				response.setContentType("text/html");
				writer.println("<html><body>");
				writer.println("<h1>Flex compilation error</h1>");
				writer.println("<pre>");
				writer.println(e.getMessage());
				writer.println("</pre>");
				writer.println("<p>Check server logs for more details</p>");
				writer.println("</body></html>");
				return;
			}
        }
		
		File gspFile = new File(requestedPath.substring(0, (requestedPath.length() - 5)) + ".gsp");
		if (!gspFile.exists()) {
			File htmlFile = new File(swfFolder, swfPath.substring(swfPath.lastIndexOf(File.separator)+1) + ".html");
			if (htmlFile.exists())
				htmlFile.renameTo(gspFile);
		}
        
        response.setContentType("application/x-shockwave-flash");
        response.setContentLength((int)swfFile.length());
        response.setBufferSize((int)swfFile.length());
        response.setDateHeader("Expires", 0);
        
        OutputStream os = null;
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(swfFile));
            os = response.getOutputStream();
            for (int b = is.read(); b != -1; b = is.read())
                os.write(b);
        } finally {
            if (is != null) try {
                is.close();
            } finally {
                if (os != null)
                    os.close();
            }
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
    
    
    protected long lastModified(File path, final String[] extensions) {
    	long lastModified = 0L;
    	String[] files = path.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String ext : extensions) {
					if (name.endsWith(ext))
						return true;
				}
				return false;
			}
    	});
    	for (String file : files) {
    		File f = new File(path, file);
    		if (f.isDirectory()) {
    			long lm = lastModified(f, extensions);
    			if (lm > lastModified)
    				lm = lastModified;
    		}
    		else if (f.lastModified() > lastModified)
    			lastModified = f.lastModified();
    	}
    	
    	return lastModified;
    }


   @Override
    public void destroy() {
       servletConfig = null;
    }
}
