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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author maber01
 */
public class FileProcessor
{
  private static int nextKeyInt = 1000000;
  private static final HashMap<String,FileProcessor> instanceMap = new HashMap<>();
  
  public static String processFile( String id, String srcrel, String dstrel )
  {
    FileProcessor instance;
    synchronized( instanceMap )
    {
      instance = instanceMap.get( id );
    }
    if ( instance == null )
      return "Unknown FileProcessor instance ID.";
    return instance.processFile( srcrel, dstrel );
  }

  
  
  private final String id;
  private final Path srcbase;
  private final Path dstbase;
  public FileProcessor( Path srcbase, Path dstbase )
  {
    this.srcbase = srcbase;
    this.dstbase = dstbase;
    synchronized( instanceMap )
    {
      this.id = "fileprocessor" + Integer.toString( nextKeyInt++ );
      instanceMap.put( id, this );
    }
  }
  
  public String getId()
  {
    return id;
  }
  
  public void release()
  {
    synchronized( instanceMap )
    {
      if ( instanceMap.containsKey( id ) )
        instanceMap.remove( this.id );
    }
  }  
  
  public String processFile( String srcrel, String dstrel )
  {
    StringBuilder sb = new StringBuilder();
    try
    {
//      System.out.println( "    Src Base " + srcbase );
//      System.out.println( "    Dst Base " + dstbase );
//      System.out.println( "    Src Rel " + srcrel );
//      System.out.println( "    Dst Rel " + dstrel );
      Path src = srcbase.resolve( srcrel );
      Path dst = dstbase.resolve( dstrel ).resolve( srcrel );
//      System.out.println( "    Src Path " + src.toString() );
//      System.out.println( "    Dst Path " + dst.toString() );
      System.out.println( "    Src URI " + src.toUri() );
      System.out.println( "    Dst URI " + dst.toUri() );
      sb.append( "copy {" );
      sb.append( src.toString() );
      sb.append( "} to {" );
      sb.append( dst.toString() );
      sb.append( "} " );
      Files.createDirectories( dst.getParent() );
      IOUtils.copy( Files.newInputStream( src ), Files.newOutputStream( dst ) );
      sb.append( "success" );
    }
    catch ( IOException ex )
    {
      ex.printStackTrace();
      sb.append( " failed" );
    }
    return sb.toString();
  }
  
}
