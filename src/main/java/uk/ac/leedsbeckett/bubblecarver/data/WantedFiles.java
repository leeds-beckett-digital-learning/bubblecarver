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
package uk.ac.leedsbeckett.bubblecarver.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author maber01
 */
public class WantedFiles
{
  private final ArrayList<Path> forCombiner = new ArrayList<>();
  private final ArrayList<Path> forSplitter = new ArrayList<>();
  
  public void addForCombiner( Path p )
  {
    forCombiner.add( p );
  }

  public void addForSplitter( Path p )
  {
    forSplitter.add( p );
  }
  
  public Collection<Path> getForCombiner()
  {
    return forCombiner;
  }
  
  public Collection<Path> getForSplitter()
  {
    return forSplitter;
  }
}
