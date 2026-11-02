/*
 * Copyright 2026 maber01.
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
package uk.ac.leedsbeckett.bubblecarver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author maber01
 */
public class BubbleCarverConfig
{
  public static final String NAME_7ZAPATH = "7za.path";
  public static final String NAME_INPUTPATH = "path.input";
  public static final String NAME_OUTPUTPATH = "path.output";
  
  final Path homePath;
  File configfile;
  final Properties properties = new Properties();
  Path inputPath, outputPath;

  public BubbleCarverConfig()
  {
    File home = FileSystemView.getFileSystemView().getHomeDirectory();
    Path p = Paths.get( home.toURI() );
    homePath =  p.endsWith( "Desktop" ) ? p.getParent() : p;
    configfile = new File( homePath.toFile(), ".bubblecarver/config.properties" );
  }
  
  public String get7ZaExecutableFileName()
  {
    return properties.getProperty( NAME_7ZAPATH );
  }

  public Path getInputPath() {
    return inputPath;
  }

  public void setInputPath(Path inputPath) {
    this.inputPath = inputPath;
    properties.setProperty( NAME_INPUTPATH, inputPath.toUri().toString() );
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(Path outputPath) {
    this.outputPath = outputPath;
    properties.setProperty( NAME_OUTPUTPATH, outputPath.toUri().toString() );
  }
  
  public void load() throws IOException
  {
    if ( configfile.exists() && configfile.isFile() )
    {
      properties.load( new FileReader( configfile ) );
      inputPath = uriStringToPath( properties.getProperty( NAME_INPUTPATH ) );
      outputPath = uriStringToPath( properties.getProperty( NAME_OUTPUTPATH ) );
    }
    else
    {
     properties.setProperty( NAME_7ZAPATH, "" );
     setInputPath( homePath );
     setOutputPath( homePath );
     save();
    }
  }
  
  public void save() throws IOException
  {
    configfile.getParentFile().mkdirs();
    try ( OutputStream out = new FileOutputStream( configfile ) )
    {
      properties.store( out, "Bubblecarver configuration file.");
    }
  }
  
  public Path uriStringToPath( String s )
  {
    if ( StringUtils.isBlank( s ) )
      return homePath;
    URI uri = URI.create( s );
    return Path.of( uri );
  }
}
