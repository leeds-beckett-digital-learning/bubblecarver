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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author maber01
 */
public class ResourceUsageSet
{
  public static final String ALL_STUDENTS_GID = "allstudents";
  
  public final HashMap<String,ResourceUsageRecord> map = new HashMap<>();
  public void addGid( String resid, String gid )
  {
    getCreateRecord( resid ).addGid( gid );
  }
  public void addSid( String resid, String sid )
  {
    getCreateRecord( resid ).addSid( sid );
  }
  public boolean isGidUser( String resid, String gid )
  {
    ResourceUsageRecord record = map.get( resid );
    if ( record == null ) return false;
    return record.isGidUser( gid );
  }
  public boolean isSidUser( String resid, String sid )
  {
    // This class doesn't know list of all students
    // so check this first.
    if ( isGidUser( resid, ALL_STUDENTS_GID ) )
      return true;
    
    ResourceUsageRecord record = map.get( resid );
    if ( record == null ) return false;
    return record.isSidUser( sid );
  }
  
  private ResourceUsageRecord getCreateRecord( String resid )
  {
    if ( map.containsKey( resid ) )
      return map.get( resid );
    ResourceUsageRecord record = new ResourceUsageRecord();
    map.put( resid, record );
    return record;
  }
  
  /**
   * Resolves group membership to work out which students are
   * using group based resources such as group attempts.
   * 
   * @param groupMap 
   */
  public void rebuild( GroupMap groupMap )
  {
    map.forEach
    (
      ( resid, record ) ->
        {
          // Cannot build all students group because this task
          // doesnt know who all students are. So isSidUser must
          // take this into account.
          if ( !ALL_STUDENTS_GID.equals( resid ) )
          {
            Set<String> sids = groupMap.getStudentsInGroups( record.gidSet );
            sids.forEach
            ( 
              sid -> 
              addSid( resid, sid )
            );
          }
        }
    );
  }
  
  public synchronized void dump( Path file )
  {
    try ( PrintWriter writer = new PrintWriter( new FileWriter( file.toFile() ) ) )
    {
      writer.println( "ResourceUsage" );
      writer.println( "=============" );
      map.forEach( (resid, record) ->
          {
            writer.println( "  Resource " + resid );
            writer.println( "  ---------------" );
            writer.println( "    Student IDs" );
            writer.println( "    ..........." );
            record.sidSet.forEach( s -> writer.println( "      " + s ) );
            writer.println( "    Group IDs" );
            writer.println( "    ..........." );
            record.gidSet.forEach( g -> writer.println( "      " + g ) );
          }
      );
    }
    catch (IOException ex)
    {
      System.getLogger( ResourceUsageSet.class.getName()).log(System.Logger.Level.ERROR, "Unable to dump data.", ex);
    }    
  }
}
