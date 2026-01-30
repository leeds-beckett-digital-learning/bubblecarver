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

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author maber01
 */
public class DataLookups
{
  private final HashMap<String,HashMap<String,String>> parentmapmap = new HashMap<>();

  public HashMap<String,String> getMap( String type )
  {
    return parentmapmap.get( type );
  }
  
  public String setParentChild( String type, String parent, String child )
  {
    if ( type   == null || type.length()   == 0 || 
         parent == null || parent.length() == 0 || 
         child  == null || child.length()  == 0 )
      return "parent: " + parent + " child: " + child;
    HashMap<String,String> parentmap = parentmapmap.get( type );
    if ( parentmap == null )
    {
      parentmap = new HashMap<>();
      parentmapmap.put( type, parentmap );
    }
    parentmap.put( child, parent );
    return "parent: " + parent + " child: " + child;
  }

  public String getParentOfChild( String type, String child )
  {
    if ( type  == null || type.length()  == 0 || 
         child == null || child.length() == 0 )
      return "unknown";
    HashMap<String,String> parentmap = parentmapmap.get( type );
    if ( parentmap == null ) return "unknown";
    String parent = parentmap.get( child );
    if ( parent == null || parent.length() == 0 ) return "unknown";
    return parent;
  }
  
  public void dump()
  {
    for ( String type : parentmapmap.keySet() )
    {
      System.out.println( "Map type: " + type );
      HashMap<String,String> map = parentmapmap.get( type );
      for ( String child : map.keySet() )
        System.out.println( "    child: " + child + "   parent: " + map.get( child ) );
    }
  }
}
