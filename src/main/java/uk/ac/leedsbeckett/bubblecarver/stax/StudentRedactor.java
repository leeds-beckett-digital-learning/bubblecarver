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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;

/**
 *
 * @author maber01
 */
public class StudentRedactor implements MatchProcessor
{
  final static QName IDATTRIBUTE = new QName( "id" );
  final static QName IDENTIFIERATTRIBUTE = new QName( "identifier" );
  final static QName HREFATTRIBUTE = new QName( "href" );
  final static QName TYPEATTRIBUTE = new QName( "type" );
  
  static StudentRedactorMatcher[] matchers = 
  {
    new StudentRedactorMatcher( 
            new String[]{ Mode.OMIT.getName() }, 
            new String[]{}, 
            StudentRedactor::processOmitted
    ),
    new StudentRedactorMatcher( 
            new String[]{}, 
            new String[]{ "/resources/resource" }, 
            StudentRedactor::processResource
    ),
    new StudentRedactorMatcher( 
            new String[]{ Mode.QTIATTEMPT.getName() }, 
            new String[]{ "/resources/resource/data" }, 
            StudentRedactor::processDataQtiAttempt
    ),
    new StudentRedactorMatcher( 
            new String[]{ Mode.QTIATTEMPT.getName() }, 
            new String[]{ "/resources/resource/file" }, 
            StudentRedactor::processFileQtiAttempt
    ),
    new StudentRedactorMatcher( 
            new String[]{ Mode.ATTEMPTFILE.getName() }, 
            new String[]{ "/resources/resource/data/ATTEMPTFILES/ATTEMPTFILE" }, 
            StudentRedactor::processDataAttemptFile
    ),
    new StudentRedactorMatcher( 
            new String[]{ Mode.ATTEMPTFILE.getName() }, 
            new String[]{ "/resources/resource/file" }, 
            StudentRedactor::processFileAttemptFile
    ),
    new StudentRedactorMatcher( 
            new String[]{ Mode.ATTEMPTEMBEDFILE.getName() }, 
            new String[]{ "/resources/resource/data" }, 
            StudentRedactor::processDataAttemptFile
    ),
    new StudentRedactorMatcher( 
            new String[]{ Mode.ATTEMPTEMBEDFILE.getName() }, 
            new String[]{ "/resources/resource/file" }, 
            StudentRedactor::processFileAttemptFile
    ),
    new StudentRedactorMatcher( 
            new String[]{}, 
            new String[]{}, 
            StudentRedactor::processOther
    ),
          
  };
  
  EventReaderContext erContext = new EventReaderContext( matchers );
  
  private final Path source;
  final FileWriter fileWriter;
  final XMLOutputFactory factory = XMLOutputFactory.newInstance();
  XMLStreamWriter writer;

  private final GeneralMap generalMap;
  private final String uid, sid, mid;
  
  int outputdepth=0;
  String currentResourceType;
  String currentResourceIdentifier;
  
  public StudentRedactor( 
          Path source, GeneralMap generalMap, 
          String uid, String sid, String mid, 
          Path outputPath ) throws IOException
  {
    this.source = source;
    this.generalMap = generalMap;
    this.uid = uid;
    this.sid = sid;
    this.mid = mid;
    fileWriter = new FileWriter( outputPath.toFile() );
  }

  String indent() throws XMLStreamException
  {
    return " ".repeat( 4*outputdepth );
  }
  
  
  public void process() throws IOException, XMLStreamException
  {
    writer = factory.createXMLStreamWriter(fileWriter);
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeCharacters( "\n" );
    
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    XMLEventReader docreader = xmlInputFactory.createXMLEventReader( Files.newInputStream( source ) );
    while( docreader.hasNext() )
    {
      XMLEvent nextEvent = docreader.nextEvent();
      switch ( nextEvent.getEventType() )
      {
        case XMLEvent.START_ELEMENT ->
        {
          erContext.push( nextEvent.asStartElement() );
          StudentRedactorMatcher matcher = (StudentRedactorMatcher)erContext.firstMatch();
          if ( matcher != null )
            matcher.consumer.accept( this, erContext.getCurrentContext() );
          if ( erContext.getCurrentContext().getProperty( "foroutput" ) != null )
          {
            //System.out.println( indent() + "Output start element " + erContext.getCurrentContext().path );
            writeStartElementCopy( nextEvent.asStartElement() );
            if ( outputdepth == 0 )
            {
              writer.writeAttribute( "uid", uid );
              writer.writeAttribute( "sid", sid );
              writer.writeAttribute( "mid", mid );
            }
            outputdepth++;
          }
        }
        case XMLEvent.END_ELEMENT ->
        {
          if ( erContext.getCurrentContext().getProperty( "foroutput" ) != null )
          {
            outputdepth--;
            //System.out.println( indent() + "Output end element " + erContext.getCurrentContext().path );
            writer.writeEndElement();
          }
          erContext.pop();
        }
        case XMLEvent.CHARACTERS ->
        {
          processCharacters( nextEvent );
        }
        case XMLEvent.CDATA ->
        {
          processCharacters( nextEvent );
        }
      }
    }
    
    writer.writeEndDocument();
    writer.flush();
    writer.close();
    fileWriter.close();    
  }
  
  void processCharacters( XMLEvent nextEvent ) throws XMLStreamException
  {
    if ( erContext.size() == 0 ||
         erContext.getCurrentContext().getProperty( "foroutput" ) != null )
    {
      if ( nextEvent.asCharacters().isCData() )
        writer.writeCData( nextEvent.asCharacters().getData() );
      else
        writer.writeCharacters( nextEvent.asCharacters().getData() );            
    }    
  }
  
  void writeStartElementCopy( StartElement event ) throws XMLStreamException
  {
    writer.writeStartElement( event.getName().getPrefix(), event.getName().getLocalPart(), event.getName().getNamespaceURI() );
    for ( Iterator<Namespace> i = event.getNamespaces(); i.hasNext(); )
    {
      Namespace ns = i.next();
      writer.writeNamespace( ns.getPrefix(), ns.getNamespaceURI() );
    }
    for ( Iterator<Attribute> i = event.getAttributes(); i.hasNext(); )
    {
      Attribute a = i.next();
      writer.writeAttribute( a.getName().getPrefix(), a.getName().getNamespaceURI(), a.getName().getLocalPart(), a.getValue() );
    }
  }
  
  public static void processResource( MatchProcessor r, ElementContext context )
  {
    StudentRedactor redactor = (StudentRedactor) r;
    
    Attribute aType = context.element.getAttributeByName( TYPEATTRIBUTE );
    if ( aType == null )
      throw new IllegalArgumentException( "Resource in XML with no type attrinbute!" );
    redactor.currentResourceType = aType.getValue();
    aType = context.element.getAttributeByName( IDENTIFIERATTRIBUTE );
    if ( aType == null )
      throw new IllegalArgumentException( "Resource in XML with no identifier attrinbute!" );
    redactor.currentResourceIdentifier = aType.getValue();
            
    if ( null == redactor.currentResourceType )
    {
      // Temporarily ignore all other types of resource.
      context.mode = Mode.OMIT.getName();
    }
    else switch ( redactor.currentResourceType )
    {
      case "course/x-bb-qti-attempt" ->
      {
        context.putProperty( "foroutput", "true" );
        context.mode = Mode.QTIATTEMPT.getName();
      }
      case "course/x-bb-attemptfiles" ->
      {
        context.putProperty( "foroutput", "true" );
        context.mode = Mode.ATTEMPTFILE.getName();
      }
      default -> // Temporarily ignore all other types of resource.
        context.mode = Mode.OMIT.getName();
    }
  }
  
  public static void processDataQtiAttempt( MatchProcessor r, ElementContext context )
  {
    // Dump all the data (only interested in the files)
    context.mode = Mode.OMIT.getName();
  }
  
  public static void processFileQtiAttempt( MatchProcessor r, ElementContext context )
  {
    StudentRedactor redactor = (StudentRedactor) r;
    String href = context.element.getAttributeByName( HREFATTRIBUTE ).getValue();
    String[] split = href.split( "/" );
    String resultobjectid = split[0];
    String memberid = redactor.generalMap.get( "result_membership", resultobjectid );
    if ( redactor.mid.equals( memberid ) )
      processOther( r, context );
    else
      context.mode = Mode.OMIT.getName();
  }

  public static void processDataAttemptFile( MatchProcessor r, ElementContext context )
  {
    StudentRedactor redactor = (StudentRedactor) r;
    String fid = context.element.getAttributeByName( IDATTRIBUTE ).getValue();
    String attemptid = redactor.generalMap.get( "file_attempt", fid );
    String memberid = redactor.generalMap.get( "attempt_membership", attemptid );
    if ( redactor.mid.equals( memberid ) )
      processOther( r, context );
    else
      context.mode = Mode.OMIT.getName();
  }
  
  public static void processFileAttemptFile( MatchProcessor r, ElementContext context )
  {
    StudentRedactor redactor = (StudentRedactor) r;
    String href = context.element.getAttributeByName( HREFATTRIBUTE ).getValue();
    String[] split = href.split( "/" );
    String fid = split[0];
    String attemptid = redactor.generalMap.get( "file_attempt", fid );
    String memberid = redactor.generalMap.get( "attempt_membership", attemptid );
    if ( redactor.mid.equals( memberid ) )
      processOther( r, context );
    else
      context.mode = Mode.OMIT.getName();
  }

  public static void processDataAttemptEmbedFile( MatchProcessor r, ElementContext context )
  {
    StudentRedactor redactor = (StudentRedactor) r;
    String attemptid = redactor.generalMap.get( "resource_attempt", redactor.currentResourceIdentifier );
    String memberid = redactor.generalMap.get( "attempt_membership", attemptid );
    if ( redactor.mid.equals( memberid ) )
      processOther( r, context );
    else
      context.mode = Mode.OMIT.getName();
  }
  
  public static void processFileAttemptEmbedFile( MatchProcessor r, ElementContext context )
  {
    StudentRedactor redactor = (StudentRedactor) r;
    String href = context.element.getAttributeByName( HREFATTRIBUTE ).getValue();
    String[] split = href.split( "/" );
    String fid = split[0];
    String attemptid = redactor.generalMap.get( "file_attempt", fid );
    String memberid = redactor.generalMap.get( "attempt_membership", attemptid );
    if ( redactor.mid.equals( memberid ) )
      processOther( r, context );
    else
      context.mode = Mode.OMIT.getName();
  }

  public static void processOmitted( MatchProcessor r, ElementContext context )
  {
  }
  
  public static void processOther( MatchProcessor r, ElementContext context )
  {
    context.putProperty( "foroutput", "true" );
  }
  
  
  enum Mode
  {
    QTIATTEMPT( "qtiattempt" ),
    ATTEMPTFILE( "attemptfile" ),
    ATTEMPTEMBEDFILE( "attemptembedfile" ),
    OMIT( "omit" ),
    OTHER( "other" );
    
    private final String name;
    Mode( String name )
    {
      this.name = name;
    }
    
    public String getName()
    {
      return name;
    }
  }
  
  static class StudentRedactorMatcher implements Matcher
  {
    final String[] modes;
    final String[] xpaths;
    final BiConsumer<MatchProcessor,ElementContext> consumer;

    public StudentRedactorMatcher(String[] modes, String[] xpaths, BiConsumer<MatchProcessor, ElementContext> consumer)
    {
      this.modes = modes;
      this.xpaths = xpaths;
      this.consumer = consumer;
    }
    
    @Override
    public String[] getModes()
    {
      return modes;
    }

    @Override
    public String[] getXPaths()
    {
      return xpaths;
    }

    @Override
    public BiConsumer<MatchProcessor,ElementContext> getConsumer()
    {
      return consumer;
    }
  }
}
