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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import uk.ac.leedsbeckett.bubblecarver.stax.*;

/**
 *
 * @author maber01
 * @param <F>
 * @param <R>
 * @param <W>
 */
public abstract class StaxFilter<F extends FilterContext, R extends ReaderNodeContext, W extends WriterNodeContext>
        implements MatchProcessor
{
  final XMLEventReader reader;
  final XMLStreamWriter writer;
        List<FilterMatcher> matchers;
  final FullFilterContext<F,R,W> context;
  final StaxFilterConfiguration config;
  
  public StaxFilter( 
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<F,R,W> context,
          StaxFilterConfiguration config )
  {
    this.reader = reader;
    this.writer = writer;
    this.context = context;
    this.config = config;
  }

  public void setMatchers( FilterMatcher[] matchers)
  {
    this.matchers = Arrays.asList( matchers );
  }

  public XMLStreamWriter getWriter()
  {
    return writer;
  }
  
  public FullFilterContext<F, R, W> getContext()
  {
    return context;
  }

  public StaxFilterConfiguration getConfig() {
    return config;
  }

  public XMLEvent peek() throws XMLStreamException
  {
    return reader.peek();
  }
  
  public void process() throws XMLStreamException, IOException
  {
    if ( !config.outputFragment )
      writeProlog();
    
    XMLEvent current = null;
    XMLEvent previous = null;
    while( reader.hasNext() )
    {
      previous = current;
      current = reader.nextEvent();

      switch ( current.getEventType() )
      {
        case XMLEvent.ATTRIBUTE      -> {}  // Here to prevent exception on default clause
        case XMLEvent.COMMENT        -> {}  // ditto
        case XMLEvent.DTD            -> {}
        case XMLEvent.START_DOCUMENT -> {}
        case XMLEvent.END_DOCUMENT   -> {}
        case XMLEvent.START_ELEMENT  -> preStartElement( current.asStartElement() );
        case XMLEvent.END_ELEMENT    -> {}
        case XMLEvent.CHARACTERS     -> {}
        case XMLEvent.CDATA          -> {}
        default -> throw new IllegalArgumentException( "Cannot handle STAX event type " + current.getEventType() );
      }

      R node = context.getReaderNodeContext();
      if ( node != null )
      {
        node.setPreviousEvent( previous );
        node.setNextEvent( reader.peek() );
      }

      switch ( current.getEventType() )
      {
        case XMLEvent.ATTRIBUTE      -> handleAttribute( current );  // atts handled by start element event
        case XMLEvent.COMMENT        -> handleComment( current );
        case XMLEvent.DTD            -> handleDTD( current );
        case XMLEvent.START_DOCUMENT -> handleStartDocument( current );
        case XMLEvent.END_DOCUMENT   -> handleEndDocument( current );        
        case XMLEvent.START_ELEMENT  -> handleStartElement( context.getReaderNodeContext() );
        case XMLEvent.END_ELEMENT    -> handleEndElement( context.getReaderNodeContext() );
        case XMLEvent.CHARACTERS     -> handleCharacters( context.getReaderNodeContext(), current.asCharacters()   );
        case XMLEvent.CDATA          -> handleCharacters( context.getReaderNodeContext(), current.asCharacters()   );
      }
      
      switch ( current.getEventType() )
      {
        case XMLEvent.START_ELEMENT  -> postStartElement( context.getReaderNodeContext() );
      }
    }
    reader.close();
    
  }
  
  public void handleAttribute( XMLEvent event ) throws XMLStreamException
  {
  }
  
  public void handleComment( XMLEvent event ) throws XMLStreamException
  {
  }
  
  public void handleDTD( XMLEvent event ) throws XMLStreamException
  {
  }
  
  public void handleStartDocument( XMLEvent event ) throws XMLStreamException
  {
  }
  
  public void handleEndDocument( XMLEvent event ) throws XMLStreamException
  {
  }
  
  public void preStartElement( StartElement event ) throws XMLStreamException
  {
    R parent = ( context.getReaderNodeDepth() > 0 ) ? context.getReaderNodeContext() : null;
    R node = createReaderNode();

    node.setStartElement( event );
    
    // build local part name path
    StringBuilder sb = new StringBuilder();
    if ( parent != null )
      sb.append( parent.getPath() );
    sb.append( "/" );
    sb.append( event.getName().getLocalPart() );
    node.setPath( sb.toString() );
    
    // inherit mode from parent or none (handleStartElement can change it later.)
    node.setMode( (parent != null) ? parent.getMode() : "" );
    
    // Make this the new current node
    context.pushReaderNodeContext( node );
  }
  
  public void handleStartElement( ReaderNodeContext node ) throws XMLStreamException, IOException
  {
    FilterMatcher m = firstMatch();
    
    if ( m == null )
    {
      // Create an output plan by cloning current node
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      // Make a record of it
      node.setWritePlan( plan );
      return;
    }
    
    // Otherwise
    try
    {
      m.getHandler().handleStartElement();
    }
    catch ( Exception e )
    {
      Location l = node.getStartElement().getLocation();
      System.out.println( 
              "Location of exception, Line: " + l.getLineNumber() + 
                                  " Column: " + l.getColumnNumber() );
      throw e;
    }
  }
  
  public void postStartElement( ReaderNodeContext node ) throws XMLStreamException
  {
    ElementWritePlan planned = node.getWritePlan();
    ElementWritePlan written = node.getWrittenPlan();
    // if a plan was set but subclass handler didn't send it yet
    // send it now.
    if ( planned != null && written == null )
      writeStartElement( node );
  }
  
  public void handleEndElement( ReaderNodeContext node ) throws XMLStreamException
  {
    ElementWritePlan plan = node.getWritePlan();
    if ( plan != null )
      writeEndElement( plan );
    // finished 
    context.popReaderNodeContext();
  }
  
  public void handleCharacters( ReaderNodeContext node, Characters event ) throws XMLStreamException
  {
    // If the containing element is being written, also write character children
    if ( node.getWritePlan() != null )
    {
      if ( event.isCData() )
        writer.writeCData( event.getData() );
      else
        writer.writeCharacters( event.getData() );
    }
  }
  
  public void writeProlog() throws XMLStreamException
  {
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeCharacters( "\n" );  
  }
  
  public void writeStartElement( ReaderNodeContext node ) throws XMLStreamException
  {
    writeStartElement( node.getWritePlan() );
    node.setWrittenPlan( node.getWritePlan() );
  }
  
  public void writeStartElement( ElementWritePlan plan ) throws XMLStreamException
  {
    QName n = plan.getName();
    writer.writeStartElement( n.getPrefix(), n.getLocalPart(), n.getNamespaceURI() );
    for ( Namespace ns : plan.getNamespaceList() )
      writer.writeNamespace( ns.getPrefix(), ns.getNamespaceURI() );
    for ( Attribute a : plan.getAttributeList() )
    {
      QName an = a.getName();
      try
      {
        // Fix prefix if necessary
        String prefix;
        if ( !an.getNamespaceURI().isEmpty() && an.getPrefix().isEmpty() )
          prefix = writer.getNamespaceContext().getPrefix( an.getNamespaceURI() );
        else
          prefix = an.getPrefix();
        writer.writeAttribute( prefix, an.getNamespaceURI(), an.getLocalPart(), a.getValue() );
      }
      catch ( XMLStreamException xex )
      {
        throw xex;
      }
    }
  }
  
  void writeEndElement( ElementWritePlan plan ) throws XMLStreamException
  {
    // does the plan make any different here?
    // writer automatically uses the paired element name
    writer.writeEndElement();
  }
  
  
  /**
   * Cannot handle predicates, namespaces, ns prefixes, axes.....
   * Just element names from root
   * 
   * @return 
   */
  FilterMatcher firstMatch()
  {
    if ( matchers == null )
      return null;
    for ( FilterMatcher matcher : matchers )
      if ( context.isReaderNodeContextMatch( matcher.getModes(), matcher.getXPaths() ) )
        return matcher;
    return null;
  }

  public abstract R createReaderNode();
  public abstract W createWriterNode();
}
