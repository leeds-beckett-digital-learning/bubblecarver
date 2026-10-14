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
package uk.ac.leedsbeckett.bubblecarver.zip;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 *
 * @author maber01
 */
public class ZipProcessor
{
  final private String zipExecPath;
  final private Path zipPath;
  final private Path targetPath;

  final private LinkedList<String> relativePaths = new LinkedList<>();
  
  public ZipProcessor( String zipExecPath, Path zipPath, Path targetPath )
  {
    this.zipExecPath = zipExecPath;
    this.zipPath = zipPath;
    this.targetPath = targetPath;
  }

  public void list() throws IOException
  {
    String[] commandandargs = { zipExecPath, "l", "-slt", "-x!csfiles\\home_dir\\*", zipPath.toString() };
    execute7Zip( commandandargs );
  }
  
  public void extract( List<String> files ) throws IOException
  {
    Path flp = targetPath.resolve( "filelist.txt" );
    try ( FileWriter writer = new FileWriter( flp.toFile(), StandardCharsets.UTF_8 ) )
    {
      for ( String line : files )
      {
        writer.write( line );
        writer.write( "\r\n" );
      }
    }
    // always answer yes to overwrite question
    // only extract files listed in filelist.txt
    // extract to current directory
    String[] commandandargs = { zipExecPath, "x", "-y", "-i@filelist.txt", zipPath.toString() };
    execute7Zip( commandandargs );    
  }
   
  public void execute7Zip( String[] commandandargs ) throws IOException
  {
    Runtime run  = Runtime.getRuntime();
    String[] env = {};
    // ensure current directory is at target path
    Process process = run.exec( commandandargs, env, targetPath.toFile() );
    try ( BufferedReader reader = new BufferedReader(new InputStreamReader( process.getInputStream() ) ) )
    {
      String line;
      while ( (line = reader.readLine()) != null )
      {
        System.out.println( line );
      }          
    }
    System.out.println( "Exit code from 7za process = " + process.exitValue() );
    if ( process.exitValue() != 0 )
      throw new IOException( "7za returned error after attempting file extraction." );
  }
}
