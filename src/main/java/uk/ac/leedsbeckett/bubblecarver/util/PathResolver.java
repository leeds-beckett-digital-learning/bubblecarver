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

/**
 *
 * @author maber01
 */
public class PathResolver
{
  final Path outputBasePath;
  final Path coursesPath;

  public PathResolver(Path outputBasePath)
  {
    this.outputBasePath = outputBasePath;
    coursesPath = this.outputBasePath.resolve( "courses" );
  }

  public Path getCourseBasePath( String courseId, String packageTime )
  {
    return coursesPath.resolve( courseId + "__" + packageTime );
  }

  public Path getCompletedFilePath( Path courseBasePath )
  {
    return courseBasePath.resolve( "completed" );
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
