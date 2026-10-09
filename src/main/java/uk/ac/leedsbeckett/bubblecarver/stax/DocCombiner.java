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

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.data.WantedManifestResourceTypes;

/**
 *
 * @author maber01
 */
public class DocCombiner
{
  final static String XMLNAMESPACE = "http://www.w3.org/XML/1998/namespace";
  final static String BBNAMESPACE = "http://www.blackboard.com/content-packaging/";
  final static String BCARVNAMESPACE = "http://www.leedsbeckett.ac.uk/bubblecarver";
  final static QName XMLBASEATTRIBUTE = new QName( XMLNAMESPACE, "base" );
  final static QName IDATTRIBUTE = new QName( "id" );
  final static QName VALUEATTRIBUTE = new QName( "value" );
  final static QName TYPEATTRIBUTE = new QName( "type" );
  final static QName IDENTIFIERATTRIBUTE = new QName( "identifier" );
  final static QName HREFATTRIBUTE = new QName( "href" );
  final static QName BBFILEATTRIBUTE = new QName( BBNAMESPACE, "file" );
  
  
  final Path manifestPath;
  final Path basePath;
  final FileWriter fileWriter;
  XMLStreamWriter writer;
  int outputdepth=0;
  final XMLOutputFactory factory = XMLOutputFactory.newInstance();
  final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
  
  EventReaderContext manifestContext = new EventReaderContext();
  EventReaderContext resourceContext = new EventReaderContext();
  
  boolean insideSelectedResource = false;
  String currentResourceType = null;
  String currentResourceIdentifier = null;

  String currentMembershipId = null;
  
  private final GeneralMap generalMap = new GeneralMap();
  
  public DocCombiner( Path manifestPath, Path outputPath) throws IOException, XMLStreamException
  {
    this.manifestPath = manifestPath;
    this.basePath = manifestPath.getParent();
    fileWriter = new FileWriter( outputPath.toFile() );
  }

  public GeneralMap getGeneralMap()
  {
    return generalMap;
  }

  void indent() throws XMLStreamException
  {
    writer.writeCharacters( " ".repeat( 4*outputdepth )  );
  }
  
  public void process() throws XMLStreamException, FileNotFoundException, IOException
  {
    try
    {
      doProcessing();
      // Now process the file listing in the archive to find out
      // which files apply to which students.
      Stream<Path> streamSearch = Files.find( basePath, Integer.MAX_VALUE, (path,atts) -> atts.isRegularFile() );
      if ( streamSearch != null )
        streamSearch.forEach( new PathProcessor() );      
    }
    finally
    {
      
    }
  }
  
  void doProcessing() throws XMLStreamException, FileNotFoundException, IOException
  {
    XMLEventReader manifestreader = xmlInputFactory.createXMLEventReader( Files.newInputStream( manifestPath ) );

    // Start the document with the XML version and encoding information
    writer = factory.createXMLStreamWriter(fileWriter);
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeCharacters( "\n" );
    while( manifestreader.hasNext() )
    {
      XMLEvent nextEvent = manifestreader.nextEvent();
      switch ( nextEvent.getEventType() )
      {
        case XMLEvent.START_DOCUMENT  ->
        {
          writer.writeStartElement( "bbcarv", "resources", BCARVNAMESPACE );
          outputdepth++;
          writer.writeNamespace( "bb", BBNAMESPACE );
          writer.writeNamespace( "bbcarv", BCARVNAMESPACE );
          writer.writeCharacters( "\n" );
        }

        case XMLEvent.END_DOCUMENT  ->
        {
          outputdepth--;
          writer.writeEndElement();    
          writer.writeCharacters( "\n" );
        }

        case XMLEvent.START_ELEMENT  ->
        {
          manifestContext.push( nextEvent.asStartElement() );
          
          if ( manifestContext.matchesXPath( "/manifest/resources/resource" ) )
          {
            String type       = manifestContext.getCurrentStartElement().getAttributeByName( TYPEATTRIBUTE       ).getValue();
            String identifier = manifestContext.getCurrentStartElement().getAttributeByName( IDENTIFIERATTRIBUTE ).getValue();
            String xmlbase    = manifestContext.getCurrentStartElement().getAttributeByName( XMLBASEATTRIBUTE    ).getValue();
            generalMap.put( "resourcexmlbase_type", xmlbase, type );
            if ( WantedManifestResourceTypes.isWanted( type ) )
            {
              currentResourceType = type;
              currentResourceIdentifier = identifier;
              insideSelectedResource = true;
              indent();
              writer.writeStartElement( BCARVNAMESPACE, "resource" );
              outputdepth++;
              writer.writeAttribute( "type", type );
              writer.writeAttribute( "identifier", identifier );
              writer.writeAttribute( XMLNAMESPACE, "base", xmlbase );
              writer.writeCharacters( "\n" );
              indent();
              writer.writeStartElement( BCARVNAMESPACE, "data" );
              outputdepth++;
              writer.writeCharacters( "\n" );
              insertPart();
              writer.writeCharacters( "\n" );
              outputdepth--;
              indent();
              writer.writeEndElement();
              writer.writeCharacters( "\n" );
            }
          }
          else if ( insideSelectedResource && manifestContext.matchesXPath( "/manifest/resources/resource/file" ) )
          {
            String href = manifestContext.getCurrentStartElement().getAttributeByName( HREFATTRIBUTE ).getValue();
            indent();
            outputdepth++;
            writer.writeStartElement( BCARVNAMESPACE, "file" );
            writer.writeAttribute( "href", href );
            writer.writeEndElement();
            writer.writeCharacters( "\n" );
            outputdepth--;
          }
        }
        case XMLEvent.END_ELEMENT    ->
        {
          if ( manifestContext.matchesXPath( "/manifest/resources/resource" ) && insideSelectedResource )
          {
            outputdepth--;
            indent();
            writer.writeEndElement();
            writer.writeCharacters( "\n" );
            insideSelectedResource = false;
            currentResourceType = null;   
            currentResourceIdentifier = null;
          }
          manifestContext.pop();
        }
      }
    }
    manifestreader.close();

    // End the document
    writer.writeEndDocument();
    writer.flush();
    writer.close();
    fileWriter.close();
  }
  
  void insertPart() throws FileNotFoundException, XMLStreamException, IOException
  {
    if ( WantedManifestResourceTypes.isWeird( currentResourceType ) )
    {
      writer.writeCharacters( "Weird resource type not supported yet. " );
      writer.writeCharacters( currentResourceType );
      return;
    }
    
    StartElement event = manifestContext.getCurrentStartElement();
    String fileref = event.getAttributeByName( BBFILEATTRIBUTE ).getValue();
    
    // Fule Path of source based on path relative to base;
    resourceContext.reset();
    Path sourcePath = this.basePath.resolve( fileref );
    XMLEventReader reader = xmlInputFactory.createXMLEventReader( Files.newInputStream( sourcePath ) );
    while( reader.hasNext() )
    {
      XMLEvent nextEvent = reader.nextEvent();
      switch ( nextEvent.getEventType() )
      {
        case XMLEvent.ATTRIBUTE      -> donothing();  // atts handled by start element event
        case XMLEvent.COMMENT        -> donothing();
        case XMLEvent.DTD            -> donothing();
        case XMLEvent.START_DOCUMENT -> donothing();
        case XMLEvent.END_DOCUMENT   -> donothing();
        
        case XMLEvent.START_ELEMENT  -> startElement( nextEvent.asStartElement() );
        case XMLEvent.END_ELEMENT    ->   endElement( nextEvent.asEndElement()   );
        case XMLEvent.CHARACTERS     ->   characters( nextEvent.asCharacters()   );
        case XMLEvent.CDATA          ->   characters( nextEvent.asCharacters()   );
        
        default -> throw new IllegalArgumentException( "Cannot handle STAX event type " + nextEvent.getEventType() );
      }
    }
    reader.close();
    resourceContext.reset();
  }
  
  void startElement( StartElement event ) throws XMLStreamException
  {
    resourceContext.push( event );

    // potential data gathering done first..
    switch ( currentResourceType )
    {
      case "course/x-bb-user"                 -> analyseUser();
      case "membership/x-bb-coursemembership" -> analyseCourseMembership();
      case "course/x-bb-gradebook"            -> analyseGradeBookStartElement();
      case "course/x-bb-attemptfiles"         -> analyseAttemptFiles();
      case "course/x-bb-attemptembeddedfiles" -> analyseAttemptEmbedFiles();
      case "resource/x-bb-document"           -> analyseDocument();
    }
    
    // then output the element
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

  void endElement( EndElement event ) throws XMLStreamException
  {
    // Data gathering context management...?
//    switch ( currentResourceType )
//    {
//      case "course/x-bb-gradebook"            -> analyseGradeBookEndElement();
//    }
    
    // Output end element
    writer.writeEndElement();
    resourceContext.pop();
  }

  void characters( Characters event ) throws XMLStreamException
  {
    if ( event.isCData() )
      writer.writeCData( event.getData() );
    else
      writer.writeCharacters( event.getData() );
  }
  
  void donothing()
  {
    
  }
  
  
  void analyseUser()
  {
    if ( resourceContext.matchesXPath( "/USERS/USER/STUDENTID" ) )
    {
      String uid = resourceContext.getParentStartElement().getAttributeByName( IDATTRIBUTE ).getValue();
      String value = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      generalMap.put( "userid_studentid", uid, value );
    }
  }

  void analyseCourseMembership()
  {
    if ( resourceContext.matchesXPath( "/COURSEMEMBERSHIPS/COURSEMEMBERSHIP/USERID" ) )
    {
      String uid = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      String value = resourceContext.getParentStartElement().getAttributeByName( IDATTRIBUTE ).getValue();
      generalMap.put( "userid_membershipid", uid, value );
    }
  }
  
  void analyseGradeBookStartElement()
  {
    if ( resourceContext.matchesXPath( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME" ) )
    {
      currentMembershipId = null;
      return;
    }
    
    if ( resourceContext.matchesXPath( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/COURSEMEMBERSHIPID" ) )
    {
      String outcomeid = resourceContext.getParentStartElement().getAttributeByName( IDATTRIBUTE ).getValue();
      currentMembershipId = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      generalMap.put( "outcome_membership", outcomeid, currentMembershipId );
      return;
    }
    
    if ( resourceContext.matchesXPath( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT" ) )
    {
      if ( currentMembershipId == null )
        throw new IllegalArgumentException( "No membership ID at start of this gradebook outcome." );
      String attemptid = resourceContext.getCurrentStartElement().getAttributeByName( IDATTRIBUTE ).getValue();
      generalMap.put( "attempt_membership", attemptid, currentMembershipId );      
    }

    if ( resourceContext.matchesXPath( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT/RESULTOBJECTID" ) )
    {
      if ( currentMembershipId == null )
        throw new IllegalArgumentException( "No membership ID at start of this gradebook outcome." );
      String resultid = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      if ( resultid.length() > 0 )
        generalMap.put( "result_membership", resultid, currentMembershipId );      
    }
  }

  
  void analyseAttemptFiles()
  {
    if ( resourceContext.matchesXPath( "/ATTEMPTFILES/ATTEMPTFILE/ATTEMPTID" ) )
    {
      String attemptid = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      String fileid = resourceContext.getParentStartElement().getAttributeByName( IDATTRIBUTE ).getValue();
      generalMap.put( "file_attempt", fileid, attemptid );
    }
  }
  
  void analyseAttemptEmbedFiles()
  {
    if ( resourceContext.matchesXPath( "/ATTEMPTEMBEDDEDFILES/ATTEMPTEMBEDDEDFILE/ATTEMPTID" ) )
    {
      String attemptid = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      String fileid = resourceContext.getParentStartElement().getAttributeByName( IDATTRIBUTE ).getValue();
      generalMap.put( "file_attempt", fileid, attemptid );
    }
  }
  
  void analyseDocument()
  {
    if ( resourceContext.matchesXPath( "/CONTENT/URL" ) )
    {
      String url = resourceContext.getCurrentStartElement().getAttributeByName( VALUEATTRIBUTE ).getValue();
      if ( !url.isEmpty() )
        generalMap.put( "url_resource", url, currentResourceIdentifier );
    }
  }  
  
  /**
   * Works out which files referenced by the manifest belong to which
   * student and will be extracted to specific student archives.
   * 
   * Internal class so can access the DocCombiner instance members.
   */
  public class PathProcessor implements Consumer<Path>
  {
    String pathToString( Path rel )
    {
      // assume relative so omit leading slash
      return rel.toString();
    }
    
    @Override
    public void accept( Path t )
    {      
      if ( !Files.isRegularFile( t ) )
        return;
      String st = t.toString();
      Path rel = basePath.relativize( t );
      String[] parts = rel.toString().split( "/"  );
      if ( "csfiles".equals( rel.getName( 0 ).toString() ) )
        processCSFiles( rel, parts );
      else
        processResFiles( rel, parts );
    }
    
    void processCSFiles( Path rel, String[] parts )
    {
      if ( 
              "privatedoc"      .equals( parts[1] ) &&
              "gradebook"       .equals( parts[2] ) &&
              "notesAndFeedback".equals( parts[3] )
         )
      {
        if ( "attempt".equals( parts[4] ) || "attemptNotes".equals( parts[4] ) )
        {
          processAttemptFile( rel, parts );
          return;
        }
        if ( 
                "gradeHistory"     .equals( parts[4] ) || 
                "gradeHistoryNotes".equals( parts[4] ) ||
                "gradeDetail"      .equals( parts[4] ) ||
                "gradeDetailNotes" .equals( parts[4] )
           )
        {
          processOutcomeFile( rel, parts );
          return;
        }
      }
    }

    void processAttemptFile( Path rel, String[] parts )
    {
      String attemptid = parts[5];
      // Does the attempt have a member ID?
      String memberid = generalMap.get( "attempt_membership", attemptid );
      generalMap.put( "archivefile_membership", pathToString( rel ), memberid );
    }
    
    void processOutcomeFile( Path rel, String[] parts )
    {
      String outcomeid = parts[5];
      // Does the outcome have a member ID?
      String memberid = generalMap.get( "outcome_membership", outcomeid );
      generalMap.put( "archivefile_membership", pathToString( rel ), memberid );      
    }
    
    void processResFiles( Path rel, String[] parts )
    {
      if ( parts.length < 2 )
        return;
      
      String resourcetype = generalMap.get( "resourcexmlbase_type", parts[0] );
      if ( "course/x-bb-attemptfiles".equals( resourcetype ) )
      {
        processResAttemptFiles( rel, parts );
        return;
      }
      if ( "course/x-bb-attemptembeddedfiles".equals( resourcetype ) )
      {
        processResAttemptEmbeddedFiles( rel, parts );
        return;
      }
      if ( "resource/x-bb-wiki".equals( resourcetype ) )
      {
        processResWikiFiles( rel, parts );
        return;
      }
    }
    
    void processResAttemptFiles( Path rel, String[] parts )
    {
      if ( parts.length < 3 )
        return;
      String attemptfileid = parts[1];
      String attemptid = generalMap.get( "file_attempt", attemptfileid );
      String memberid  = generalMap.get( "attempt_membership", attemptid );
      generalMap.put( "archivefile_membership", pathToString( rel ), memberid );      
    }
    
    void processResAttemptEmbeddedFiles( Path rel, String[] parts )
    {
      if ( parts.length < 3 )
        return;
      String attemptid = parts[1];
      generalMap.put( "resource_attempt", currentResourceIdentifier, attemptid );
    }
    
    void processResWikiFiles( Path rel, String[] parts )
    {
      
    }
  }
  
}
