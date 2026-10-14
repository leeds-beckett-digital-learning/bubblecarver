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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.swing.filechooser.FileSystemView;

/**
 *
 * @author maber01
 */
public class BubbleCarverConfig
{
  public static final String NAME_7ZAPATH = "7za.path";
  
  File configfile;
  final Properties properties = new Properties();

  public BubbleCarverConfig()
  {
    File home = FileSystemView.getFileSystemView().getHomeDirectory();
    Path path = Paths.get( home.toURI() );
    if ( path.endsWith( "Desktop" ) )
      path = path.getParent();
    configfile = new File( path.toFile(), ".bubblecarver/config.properties" );
  }
  
  public String get7ZaExecutableFileName()
  {
    return properties.getProperty( NAME_7ZAPATH );
  }
  
  public void load() throws IOException
  {
    if ( configfile.exists() && configfile.isFile() )
    {
      properties.load( new FileReader( configfile ) );
    }
    else
    {
     properties.setProperty( NAME_7ZAPATH, "" );
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
}
