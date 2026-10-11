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
package uk.ac.leedsbeckett.bubblecarver.staxfilter;

import java.util.LinkedList;

/**
 * Minimal functionality for StaxFilter
 * @author maber01
 * @param <F>
 * @param <R>
 * @param <W>
 */
public class FullFilterContext< F extends FilterContext, R extends ReaderNodeContext, W extends WriterNodeContext>
{
  final F filterContext;
  final LinkedList<R> readerNodeAxis = new LinkedList<>();
  final LinkedList<W> writerNodeAxis = new LinkedList<>();

  public FullFilterContext( F filterContext )
  {
    this.filterContext  = filterContext;
  }

  public F getFilterContext()
  {
    return filterContext;
  }
  
  public int getReaderNodeDepth()
  {
    return readerNodeAxis.size();
  }
  
  public int getWriterNodeDepth()
  {
    return writerNodeAxis.size();
  }
  
  public void pushReaderNodeContext( R node )
  {
    readerNodeAxis.push( node );
  }
  
  public void pushWriterNodeContext( W node )
  {
    writerNodeAxis.push( node );
  }
  
  public void popReaderNodeContext()
  {
    readerNodeAxis.pop();
  }
  
  public void popWriterNodeContext()
  {
    writerNodeAxis.pop();
  }
  
  public R getReaderNodeContext()
  {
    return getReaderNodeContext( 0 );
  }

  public R getReaderNodeContext( int generation )
  {
    return readerNodeAxis.get( generation );
  }

  public W getWriterNodeContext()
  {
    return getWriterNodeContext( 0 );
  }

  public W getWriterNodeContext( int generation )
  {
    return writerNodeAxis.get( generation );
  }
  
  public boolean isReaderNodeContextMatch( String[] modes, String[] xpaths )
  {
    R r = getReaderNodeContext();
    String path = r.getPath();
    
    boolean foundmode = modes.length == 0;
    if ( !foundmode )
    {
      for ( String mode : modes )
        if ( mode.equals( r.getMode() ) )
        {
          foundmode = true;
          break;
        }
    }
    if ( !foundmode )
      return false;
    // Mode match (including no mode specified) so check xpaths
    
    if ( xpaths.length == 0 )
      return true;
    
    for ( String xpath : xpaths )
    {
      // If absolute and equals whole path
      if ( xpath.charAt( 0 ) == '/' && xpath.equals( path ) )
        return true;
      else
        // If relative and equals end of path AND at / boundary
        return path.endsWith( xpath ) && path.charAt( path.length() - xpath.length() - 1 ) == '/';
    }
    
    return false;
  }
}
