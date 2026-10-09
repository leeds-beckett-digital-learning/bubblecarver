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
package uk.ac.leedsbeckett.bubblecarver.stax;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.xml.stream.events.StartElement;

/**
 *
 * @author maber01
 */
public class EventReaderContext
{
  final List<Matcher> matchers;
  final LinkedList<ElementContext> axis = new LinkedList<>();

  public EventReaderContext()
  {
    this.matchers = null;
  }
  
  public EventReaderContext( List<Matcher> matchers )
  {
    this.matchers = matchers;
  }
  
  public EventReaderContext( Matcher[] matchersarray )
  {
    this.matchers = Arrays.asList( matchersarray );
  }
  
  public void reset()
  {
    axis.clear();
  }
  
  public void push( StartElement e )
  {
    ElementContext context = new ElementContext();
    context.element = e;
    if ( size() > 0 )
    {
      context.path = getCurrentContext().path + "/" + e.getName().getLocalPart();
      // inherits mode
      context.mode = getCurrentContext().mode;
    }
    else
      context.path = "/" + e.getName().getLocalPart();
    axis.push( context );
  }
  
  public void pop()
  {
    axis.pop();
  }
  
  public int size()
  {
    return axis.size();
  }
  
  public ElementContext getCurrentContext()
  {
    return axis.getFirst();
  }
  
  public ElementContext getContext( int n )
  {
    return axis.get( axis.size() - n - 1 );
  }
  
  public StartElement getCurrentStartElement()
  {
    return axis.getFirst().element;
  }
  
  public StartElement getParentStartElement()
  {
    return axis.get( 1 ).element;
  }
  
  /**
   * Cannot handle predicates, namespaces, ns prefixes, axes.....
   * Just element names from root
   * 
   * @return 
   */
  public Matcher firstMatch()
  {
    ElementContext ec = getCurrentContext();
    for ( Matcher matcher : matchers )
    {
      boolean foundmode = matcher.getModes().length == 0;
      if ( !foundmode )
      {
        for ( String mode : matcher.getModes() )
          if ( mode.equals( ec.mode ) )
          {
            foundmode = true;
            break;
          }
      }
      boolean foundxpath = matcher.getXPaths().length == 0;
      if ( !foundxpath )
      {
        for ( String xpath : matcher.getXPaths() )
          if ( xpath.equals( ec.path ) )
          {
            foundxpath = true;
            break;
          }
      }
      if ( foundmode && foundxpath )
        return matcher;
    }
    return null;
  }
  
  @Deprecated
  public boolean matchesXPath( String xpath )
  {
    return xpath.equals( getCurrentContext().path );
  }
}
