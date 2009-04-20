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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import flex2.tools.oem.Application;
import flex2.tools.oem.Builder;
import flex2.tools.oem.Configuration;
import flex2.tools.oem.Library;
import flex2.tools.oem.Message;
import flex2.tools.oem.VirtualLocalFile;

/**
 * Webcompiler main manager
 * @author Bouiaw
 */
public class WebCompiler {
    
    private Logger logger = Logger.getLogger(WebCompiler.class);
    
    private static WebCompiler instance;
    
    private File configFile;
    
    private String basePath;
    
    private String flexPath;
    
    private String targetPlayer = "9.0.0";
    
    public static final String APPLICATION_EXTENSION = "swf";
    
    public static final String LIBRARY_EXTENSION = "swc";
    
    /**
     * Webcompiler is a singleton, this method should be used to retreive the Webcompiler instance
     * instead of using the constructor.
     */
    public static WebCompiler getInstance() {
        if (null == instance) {
            instance = new WebCompiler();
        }
        return instance;
    }
    
    /**
     * Initialize Flex webcompiler. Must be called once before compile.
     * @param basePath Directory where will be created the flex directory. For a webapp, it is usually the WEB-INF directory.
     * @throws WebCompilerException
     * @throws IOException
     */
    public void init(String basePath) throws WebCompilerException, IOException {
        
        this.basePath = basePath;
        
        this.flexPath = this.basePath + File.separator + "flex";
        
        this.createFlexDir();
        
        this.configFile = new File(this.flexPath, "flex-config.xml");
        
        if(!this.configFile.exists()) {
            throw new WebCompilerException("Missing flex-config.xml file");
        }
    }
    
    public ResourceBundle getCompilerBundle(WebCompilerType type) {
        return ResourceBundle.getBundle("compiler" + "-" + type.toString());
    }
    
    protected void createFlexDir() throws IOException {
        WebCompilerUtils.createFileFromStream("flex/flex-config.xml", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/fonts/macFonts.ser", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/fonts/winFonts.ser", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/libs/flex.swc", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/libs/framework.swc", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/libs/rpc.swc", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/libs/utilities.swc", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/libs/player/playerglobal9.swc", this.basePath, "flex/libs/player/9/playerglobal.swc");
        WebCompilerUtils.createFileFromStream("flex/libs/player/playerglobal10.swc", this.basePath, "flex/libs/player/10/playerglobal.swc");
        WebCompilerUtils.createFileFromStream("flex/locale/en_US/framework_rb.swc", this.basePath);
        WebCompilerUtils.createFileFromStream("flex/locale/en_US/rpc_rb.swc", this.basePath);
    }
    
    protected List<File> getDependencies(String name, WebCompilerType type, int majorVersion) {
        List<File> depFiles = new ArrayList<File>();
        if ("internal".equals(name)) {
            File libs = new File(this.flexPath + File.separator + "libs");
            if(libs.exists()) {
                for(File f : libs.listFiles()) {
                    if (f.isFile())
                        depFiles.add(f);
                }
            }
            File player = new File(this.flexPath + File.separator + "libs/player/" + majorVersion);
            if (player.exists()) {
                for(File f : player.listFiles()) {
                    if (f.isFile())
                        depFiles.add(f);
                }
            }
        }
        String[] values= getCompilerBundle(type).getString(name).split(",");
        for(String value : values) {
            depFiles.add(new File(this.flexPath + File.separator + value));
        }
        
        File userLibs = new File(this.flexPath + File.separator + getCompilerBundle(type).getString("user"));
        if(userLibs.exists()) {
            for(File f : userLibs.listFiles()) {
                depFiles.add(f);
            }
        }
        
        return depFiles;
    }
    
    protected void configure(Configuration configuration, WebCompilerType type) throws WebCompilerException {
        configuration.setConfiguration(this.configFile);
        
        // Set target player
        
        int[] versions = [9, 0, 0 ];
        if ( targetPlayer != null )
        {
            String[] nodes = targetPlayer.split( "\\." );
            if ( nodes.length != 3 ) {
                throw new WebCompilerException( "Invalid player version " + targetPlayer );
            }
            versions = new int[nodes.length];
            for ( int i = 0; i < nodes.length; i++ ) {
                try {
                    versions[i] = Integer.parseInt( nodes[i] );
                }
                catch ( NumberFormatException e ) {
                    throw new WebCompilerException( "Invalid player version " + targetPlayer );
                }
            }
            if ( versions[0] < 9 ) {
                throw new WebCompilerException( "Invalid player version " + targetPlayer );
            }
            configuration.setTargetPlayer( versions[0], versions[1], versions[2] );
        }
        
        List<File> internalFiles = getDependencies("internal", type, versions[0]);
        List<File> externalFiles = getDependencies("external", type, versions[0]);
        configuration.addSourcePath([new File(this.basePath)] as File[]);
        configuration.addLibraryPath(internalFiles as File[]);
        configuration.addExternalLibraryPath(externalFiles as File[]);
        
        File fontsSnapshot = getFontsSnapshot();
        if (fontsSnapshot == null || !fontsSnapshot.exists()) {
            throw new WebCompilerException("LocalFontSnapshot not found "
            + fontsSnapshot);
        }
        configuration.setLocalFontSnapshot(fontsSnapshot);
        
        configuration.setServiceConfiguration(new File(this.flexPath + File.separator + "services-config.xml"));
    }
    
    /**
     * Compile a MXML File 
     * @param mxmlFile The file that contains MXML code
     * @param outputFile The output file must have .swc or .swf extension
     * @param force Force compilation even if the output file already exists
     * @param isLibrary If true, compile the mxml code as a Flex library. If false, compile it as a Flex application. 
     * @return The output swf or swc file
     * @throws IOException
     */
    public File compileMxmlFile(File mxmlFile, File outputFile, boolean force, WebCompilerType type, String contextPath) throws IOException, WebCompilerException {
        Builder builder = null;
        
        if (!mxmlFile.exists() || !mxmlFile.isFile())
            throw new IOException("Invalid MXML file: " + mxmlFile.getCanonicalPath());
        
        File sourcePath = mxmlFile.getParentFile();
        long sourcesLastModified = 0L
        if(!force) {
            sourcesLastModified = lastModified(sourcePath,[ ".mxml", ".as" ])
        }
        long lastModified = outputFile.exists() ? outputFile.lastModified() : 0
        
        boolean changed = (!outputFile.exists() || lastModified < sourcesLastModified);
        
        if(!changed && !force) {
            logger.info("A valid swf file has been found, so skip compilation");
            return outputFile;
        }
        
        if(type==WebCompilerType.library) {
            Library lib = new Library();
            lib.addComponent(mxmlFile);
            lib.setOutput(outputFile);
            builder = lib;
        } else {
            Application app = new Application(mxmlFile);
            app.setOutput(outputFile);
            builder = app;
        }
        
        builder.setPathResolver(new WebCompilerPathResolver(this.flexPath));
        Configuration configuration = builder.getDefaultConfiguration();
        
        this.configure(configuration, type);
        configuration.setContextRoot(contextPath);
        
        builder.setConfiguration(configuration);
        
        builder.build(true);
        
        Message[] messages = builder.getReport().getMessages();
        if(messages != null) {
            boolean error = false;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Message m : messages) {
                if (Message.ERROR.equals(m.getLevel()) || Message.WARNING.equals(m.getLevel()))
                    error = true;
                sb.append(m.toString());
                // New line if it is not the last message
                if (++i != messages.length)
                    sb.append("\n");
                
            }
            if (error)
                throw new WebCompilerException(sb.toString());
        }
        
        outputFile = new File(outputFile.getCanonicalPath());
        
        return outputFile;
    }
    
    /**
     * Compile a virtual MXML file
     * @param vFile The virtual file that contains MXML code. A virtual file can be created directly from a String, without any real source file.
     * @param outputFile The output file must have .swc or .swf extension
     * @param force Force compilation even if the output file already exists
     * @param isLibrary If true, compile the mxml code as a Flex library. If false, compile it as a Flex application. 
     * @return The output swf or swc file
     * @throws IOException
     */
    public File compileMxmlVirtualFile(VirtualLocalFile vFile, File outputFile, boolean force, WebCompilerType type, String contextPath) throws IOException, WebCompilerException {
        Builder builder = null;
        
        if(type==WebCompilerType.library) {
            Library lib = new Library();
            lib.addComponent(vFile);
            lib.setOutput(outputFile);
            builder = lib;
        } else {
            Application app = new Application(vFile);
            app.setOutput(outputFile);
            builder = app;
        }
        
        builder.setPathResolver(new WebCompilerPathResolver(this.flexPath));
        Configuration configuration = builder.getDefaultConfiguration();
        
        this.configure(configuration, type);
        configuration.setContextRoot(contextPath);
        
        builder.setConfiguration(configuration);
        builder.build(true);
        
        Message[] messages = builder.getReport().getMessages();
        if(messages != null) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for(Message m : messages) {
                sb.append(m.toString());
                // New line if it is not the last message
                if(++i != messages.length)
                    sb.append("\n");
                
            }
            throw new WebCompilerException(sb.toString());
        }
        
        outputFile = new File(outputFile.getCanonicalPath());
        
        return outputFile;
    }
    
    /**
     * Get Fonts snapshot
     *
     * @return File of font snapshot
     */
    protected File getFontsSnapshot() {
        if (WebCompilerUtils.isMac()) {
            return new File(this.flexPath, "fonts/macFonts.ser");
        } else {
            // TODO And linux?!
            // if(os.contains("windows")) {
            return new File(this.flexPath, "fonts/winFonts.ser");
        }
    }
    
    private WebCompiler() {
        
    }
    
    public String getTargetPlayer() {
        return targetPlayer;
    }
    
    public void setTargetPlayer(String targetPlayer) {
        this.targetPlayer = targetPlayer;
    }
    
    
    protected long lastModified(path, extensions) {
        long lastModified = 0L;
        path.eachFileRecurse {file->
            extensions.each{ext->
                if(file.name.endsWith(ext) &&file.lastModified() > lastModified) {
                    lastModified = file.lastModified()
                }
            }
        }
        return lastModified;
    }
}
