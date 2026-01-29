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

/**
 *
 * @author maber01
 */
public abstract class XSLHelper
{
  private static int nextKeyInt = 1000000;
  private static final HashMap<String,XSLHelper> instanceMap = new HashMap<>();
  
  private final String id;

  public XSLHelper()
  {
    synchronized( instanceMap )
    {
      id = getClass().getName() + Integer.toString( nextKeyInt++ );
      instanceMap.put( id, this );
    }
  }
  
  public static <T extends XSLHelper> T get( Class<T> c, String id )
  {
    synchronized( instanceMap )
    {
      XSLHelper helper = instanceMap.get( id );
      if ( helper == null ) return null;
      if ( helper.getClass() == c )
        return (T) helper;
    }    
    return null;
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
}
