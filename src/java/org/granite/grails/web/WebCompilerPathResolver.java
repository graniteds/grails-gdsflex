/*
  GRANITE DATA SERVICES
  Copyright (C) 2007-2008 ADEQUATE SYSTEMS SARL

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

package org.granite.webcompiler;

import java.io.File;

import flex2.tools.oem.PathResolver;

/**
 * Used to resolve embedded flexsdk files.
 * @author Bouiaw
 */
public class WebCompilerPathResolver implements PathResolver {

	private String path;
	
	public WebCompilerPathResolver(String path) {
		this.path = path;
	}
	
	public File resolve(String filename) {
		File f = new File(filename);
		if(f.exists()) {
			return f;
		} else {
			f = new File(path, filename);
			if(f.exists()) {
				return f;
			}
		}
		
		return null;
	}
}
