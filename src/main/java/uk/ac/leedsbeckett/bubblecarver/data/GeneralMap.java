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

import uk.ac.leedsbeckett.bubblecarver.xsl.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author maber01
 */
public class GeneralMap
{  
  private final StringDeduplicater deduper = new StringDeduplicater();
  private final HashMap<String,HashMap<String,String>> mapomaps = new HashMap<>();
  
  private synchronized HashMap<String,String> getMap( String mapname )
  {
    if ( !mapomaps.containsKey( mapname ) )
      mapomaps.put( mapname, new HashMap<>() );
    return mapomaps.get( mapname );
  }
  
  public synchronized String put( String mapname, String key, String value )
  {
    HashMap<String,String> map = getMap( mapname );
    map.put( deduper.deduplicate(key ), deduper.deduplicate( value ) );
    return deduper.deduplicate( value );
  }
  
  public synchronized String get( String mapname, String key )
  {
    if ( !mapomaps.containsKey( mapname ) ) return null;
    return mapomaps.get( mapname ).get( key );
  }
  
  public synchronized Set<String> keySet( String mapname )
  {
    if ( !mapomaps.containsKey( mapname ) ) return new HashSet<>();
    return mapomaps.get( mapname ).keySet();
  }
  
  public synchronized void dump( Path file )
  {
    try ( FileWriter writer = new FileWriter( file.toFile() ) )
    {
      writer.append( "Start\n" );
      for ( String mapname : mapomaps.keySet() )
      {
        writer.append( "Map name = " );
        writer.append( mapname );
        writer.append( "\n" );
        HashMap<String,String> map = getMap( mapname );
        for ( String key: map.keySet() )
        {
          writer.append( "\"" );
          writer.append( key );
          writer.append( "\" = \"" );
          writer.append( map.get( key ) );
          writer.append( "\"\n" );
        }
      }
      writer.append( "End\n" );
    }
    catch (IOException ex)
    {
      System.getLogger(GeneralMap.class.getName()).log(System.Logger.Level.ERROR, "Unable to dump data.", ex);
    }
  }
}
