/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.grails.web;

import grails.util.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class GrailsWebSWFServlet extends HttpServlet {

	private static final long serialVersionUID = 1;

	protected ResourceLoader resourceLoader;
	protected ResourceLoader servletContextLoader;
	protected GrailsApplicationAttributes grailsAttributes;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		servletContextLoader = new ServletContextResourceLoader(servletConfig.getServletContext());
		ApplicationContext springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletConfig.getServletContext());
		if (springContext.containsBean(GroovyPageResourceLoader.BEAN_ID)) {
			resourceLoader = (ResourceLoader)springContext.getBean(GroovyPageResourceLoader.BEAN_ID);
		}

		grailsAttributes = new DefaultGrailsApplicationAttributes(servletConfig.getServletContext());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);

		// Get the name of the Groovy script (intern the name so that we can lock on it)
		String pageName = "/swf" + request.getServletPath();
		Resource requestedFile = getResourceForUri(pageName);
		File swfFile = requestedFile.getFile();
		if (swfFile == null || !swfFile.exists()) {
			response.sendError(404, "\"" + pageName + "\" not found.");
			return;
		}
		response.setContentType("application/x-shockwave-flash");
		response.setContentLength((int)swfFile.length());
		response.setBufferSize((int)swfFile.length());
		response.setDateHeader("Expires", 0);

		FileInputStream is = null;
		FileChannel inChan = null;
		try {
			is = new FileInputStream(swfFile);
			OutputStream os = response.getOutputStream();
			inChan = is.getChannel();
			long fSize =  inChan.size();
			MappedByteBuffer mBuf = inChan.map(FileChannel.MapMode.READ_ONLY, 0, fSize);
			byte[] buf = new byte[(int)fSize];
			mBuf.get(buf);
			os.write(buf);
		} finally {
			if (is != null)  {
				IOUtils.closeQuietly(is);
			}
			if (inChan != null) {
				try { inChan.close(); }
				catch (IOException ignored) {}
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
		if (r != null && r.exists()) {
			return r;
		}

		// try plugin
		String pluginUri = GrailsResourceUtils.WEB_INF + uri;
		r = getResourceWithinContext(pluginUri);
		if (r != null && r.exists()) {
			return r;
		}

		uri = getUriWithinGrailsViews(uri);
		return getResourceWithinContext(uri);
	}

	private Resource getResourceWithinContext(String uri) {
		Resource r = servletContextLoader.getResource(uri);
		if (r.exists()) {
			return r;
		}
		return resourceLoader != null ? resourceLoader.getResource(uri) : null;
	}

	/**
	 * Returns the path to the view of the relative URI within the Grails views directory
	 *
	 * @param relativeUri The relative URI
	 * @return The path of the URI within the Grails view directory
	 */
	protected String getUriWithinGrailsViews(String relativeUri) {
		StringBuilder buf = new StringBuilder();
		String[] tokens;
		if (relativeUri.startsWith("/")) {
			relativeUri = relativeUri.substring(1);
		}

		if (relativeUri.indexOf('/') >= 0) {
			tokens = relativeUri.split("/");
		}
		else {
			tokens = new String[] { relativeUri };
		}

		String pathToViews = GrailsApplicationAttributes.PATH_TO_VIEWS;
		if (Environment.isDevelopmentMode() && pathToViews.startsWith("/WEB-INF")) {
			pathToViews = pathToViews.substring("/WEB-INF".length());
		}

		buf.append(pathToViews);
		for (int i = 0; i < tokens.length; i++) {
			buf.append('/').append(tokens[i]);
		}

		return buf.toString();
	}

	@Override
	public void destroy() {
	}
}
