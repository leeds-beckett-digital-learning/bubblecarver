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
public class DataLookups extends XSLHelper
{  
  public static String addResultObjectId( String helperid, String mid, String roid )
  {
    DataLookups helper = get(DataLookups.class, helperid );
    if ( helper == null ) return "";
    return helper.addResultObjectId( mid, roid );
  }

  public static String isResultObjectOfMember( String helperid, String mid, String roid )
  {
    DataLookups helper = get(DataLookups.class, helperid );
    if ( helper == null ) return "false";
    return helper.isResultObjectOfMember( mid, roid );    
  }

  
  private final HashMap<String,HashSet<String>> resultobjectidmap;

  public DataLookups()
  {
    this.resultobjectidmap = new HashMap<>();
  }
  
  public DataLookups( HashMap<String,HashSet<String>> resultobjectidmap )
  {
    this.resultobjectidmap = resultobjectidmap;
  }
  
  public String addResultObjectId( String mid, String roid )
  {
    if ( mid == null || mid.length() == 0 || roid == null || roid.length() == 0 )
      return "mid: " + mid + " roid: " + roid;
    HashSet set = resultobjectidmap.get( mid );
    if ( set == null )
    {
      set = new HashSet();
      resultobjectidmap.put( mid, set );
    }
    set.add( roid );
    return "mid: " + mid + " roid: " + roid;
  }

  public String isResultObjectOfMember( String mid, String roid )
  {
    if ( mid == null || mid.length() == 0 || roid == null || roid.length() == 0 )
      return "false";
    HashSet<String> set = resultobjectidmap.get( mid );
    if ( set == null )
      return "false";
    return set.contains( roid ) ? "true" : "false";
  } 

  public HashMap<String, HashSet<String>> getResultObjectIdMap()
  {
    return resultobjectidmap;
  }
}
