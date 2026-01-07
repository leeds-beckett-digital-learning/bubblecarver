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
package uk.ac.leedsbeckett.bubblecarver.xsl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author maber01
 */
public class FileProcessor
{
  public static String processFile( String srcbase, String name, String dstbase )
  {
    StringBuilder sb = new StringBuilder();
    try
    {
      sb.append( "processFile copy from {" );
      URI dstbaseuri = new File( dstbase ).toPath().toUri();
      URI srcbaseuri = new URI( srcbase );
      URI reluri  = new URI( name );
      System.out.println( "    " + srcbaseuri.toString() );
      System.out.println( "    " + dstbaseuri.toString() );
      Path src = Path.of( srcbaseuri.resolve( reluri ) );
      Path dst = Path.of( dstbaseuri.resolve( reluri ) );
      System.out.println( "    " + src.toString() );
      System.out.println( "    " + dst.toString() );
      sb.append( src.toString() );
      sb.append( "} to {" );
      sb.append( dst.toString() );
      sb.append( "} " );
      FileUtils.createParentDirectories( dst.toFile() );
      FileUtils.copyFile( src.toFile(), dst.toFile() );
      sb.append( "success" );
    }
    catch ( URISyntaxException | IOException ex )
    {
      sb.append( " failed" );
    }
    return sb.toString();
  }
}
