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
package uk.ac.leedsbeckett.bubblecarver.util;

import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author maber01
 */
public class PathResolver
{
  final Path outputBasePath;
  final Path coursesPath;
  final Path studentsPath;
  final Path workspacePath;
  final Path zipPath;

  public PathResolver(Path outputBasePath)
  {
    this.outputBasePath = outputBasePath;
    coursesPath = this.outputBasePath.resolve( "courses" );
    studentsPath = this.outputBasePath.resolve( "students" );
    workspacePath = this.outputBasePath.resolve( "workspace" ).resolve( "blackboard" );
    zipPath = workspacePath.resolve( "ziptemp" );
  }

  public Path getBasePath()
  {
    return outputBasePath;
  }
  
  public Path getCourseBasePath( String courseId, String packageTime )
  {
    return coursesPath.resolve( courseId + "__" + packageTime );
  }

  public Path getCompletedFilePath( Path courseBasePath )
  {
    return courseBasePath.resolve( "completed" );
  }

  public Path getWorkspacePath( String name )
  {
    return workspacePath.resolve( name );
  }  
  
  public Path getCssPath( String name )
  {
    return workspacePath.resolve( "css" ).resolve( name );
  }  
  
  public Path getZipPath( String name )
  {
    return zipPath.resolve( name );
  }  
  
  public Path getStudentsPath()
  {
    return studentsPath;
  }
  
  public Path getStudentPath( String sid, String type, String course )
  {
    String longsid = sid.length() < 8 ? (sid + "_".repeat( 8 - sid.length() ) ) : sid;

    Path current = studentsPath;
    for ( int i=2; i<8; i+=2 )
    {
      String shortsid = longsid.substring( 0, i ) + "_".repeat( 8 - i );
      current = current.resolve( shortsid );
    }
    current = current.resolve( longsid );
    if ( !StringUtils.isBlank( type ) )
      current = current.resolve( type );
    if ( !StringUtils.isBlank( course ) )
      current = current.resolve( course );
    return current;
  }    
  
  public Path resolve( String name )
  {
    return outputBasePath.resolve( name );
  }

  public Path resolve( Path rel )
  {
    return outputBasePath.resolve( rel );
  }
}
