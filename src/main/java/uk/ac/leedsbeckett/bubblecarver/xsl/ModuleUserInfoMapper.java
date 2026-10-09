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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import uk.ac.leedsbeckett.bubblecarver.data.ModuleUserInfo;

/**
 *
 * @author maber01
 */
public class ModuleUserInfoMapper extends XSLHelper
{
  private final HashMap<String,ModuleUserInfo> usermap = new HashMap<>();
  
  public static String addUser( String helperid, String uid, String sid )
  {
    ModuleUserInfoMapper helper = get( ModuleUserInfoMapper.class, helperid );
    if ( helper == null ) return "";
    return helper.addUser( uid, sid );
  }

  public static String addMembership( String helperid, String uid, String mid )
  {
    ModuleUserInfoMapper helper = get( ModuleUserInfoMapper.class, helperid );
    if ( helper == null ) return "";
    return helper.addMembership( uid, mid );
  }

  public String addUser( String uid, String sid )
  {
    if ( sid == null || sid.length() < 8 )
      return "";
    ModuleUserInfo mui = new ModuleUserInfo( uid, sid );
    usermap.put( uid, mui );
    return mui.toString();
  }

  public String addMembership( String uid, String mid )
  {
    ModuleUserInfo mui = usermap.get( uid );
    if ( mui == null ) return "";
    mui.setMid( mid );
    return mui.toString();
  }
  
  public HashMap<String,ModuleUserInfo> getMap()
  {
    return usermap;
  }
  
  public void dump( Path file )
  {
    try ( FileWriter writer = new FileWriter( file.toFile() ) )
    {
      writer.append( "ModuleUserInfoMapper Dump\n" );
      for ( String uid : usermap.keySet() )
      {
        ModuleUserInfo info = usermap.get( uid );
        writer.append( "uid: " );
        writer.append( uid );
        writer.append( "  sid:" );
        writer.append( info.getSid() );
        writer.append( "  mid:" );
        writer.append( info.getMid() );
        writer.append( "\n" );
      }
    }
    catch (IOException ex)
    {
      System.getLogger(ModuleUserInfoMapper.class.getName()).log(System.Logger.Level.ERROR, "Unable to dump data.", ex);
    }
  }
}
