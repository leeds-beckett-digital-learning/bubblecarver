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
package uk.ac.leedsbeckett.bubblecarver.combiner;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class SecondPassFilter extends CombinerFilter
{
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "USERS/USER",                                       new ProcessUser() ),
    new FilterMatcherImpl( "COURSEMEMBERSHIPS/COURSEMEMBERSHIP",               new ProcessMembership() ),
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION",   new ProcessOutcomeDefinition() ),
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME",
            new ProcessOutcome() ),
    new FilterMatcherImpl( "GRADEBOOK/GRADE_HISTORY_ENTRIES/GRADE_HISTORY_ENTRY",
            new ProcessHistoryEntry() ),
    new FilterMatcherImpl( "/archive/catalogue/file",
            new ProcessCatalogueFile() ),
    new FilterMatcherImpl(                                                      new ProcessOther() )          
  };
  
  public SecondPassFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config )
  {
    super( reader, writer, context, config );
    setMatchers( matchers );
  }
    
  class ProcessUser implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String userid = node.getAttributeValue( Names.IDATTRIBUTE );
      String memberid = getGeneralMap().get( "user->member", userid );
      addUserToPlan( node, memberid );
    }
  }
  
  class ProcessMembership implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String memberid = node.getAttributeValue( Names.IDATTRIBUTE );
      addUserToPlan( node, memberid );
    }
  }
  
  class ProcessOutcomeDefinition implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String outdefid = node.getAttributeValue( Names.IDATTRIBUTE );
      
      // Get pass one contentid
      String contentid = getGeneralMap().get( "outcomedef->content", outdefid );
      // Add it to node if found
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      if ( contentid != null )
        plan.getAttributeList().add( new AttributeImpl( Names.BCARVCONTENTIDATTRIBUTE, contentid ) );
      node.setWritePlan( plan );
    }
  }

  class ProcessOutcome implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String outcomeid = node.getAttributeValue( Names.IDATTRIBUTE );
      String memberid = getGeneralMap().get( "outcome->member", outcomeid );
      addUserToPlan( node, memberid );
    }
  }

  class ProcessHistoryEntry implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String entryid = node.getAttributeValue( Names.IDATTRIBUTE );
      String userid = getGeneralMap().get( "gradehistoryentry->user", entryid );
      String memberid = getGeneralMap().get( "user->member", userid );
      addUserToPlan( node, memberid );
    }
  }

  class ProcessCatalogueFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String relativePath = node.getAttributeValue( Names.PATHATTRIBUTE );
      String[] parts = relativePath.split( "[/\\\\]" );

      // By default output file element as given
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );

      // In certain circumstances output with extra attributes
      if ( parts.length >= 1 && "csfiles".equals( parts[0] ) )
        processCsFile( node, parts );
      else
        processResourceFile( node, parts );
    }
    
    void processCsFile( ReaderNodeContext node, String[] parts )
    {
      if ( parts.length >= 4 &&
              "privatedoc".equals( parts[1] ) &&
              "gradebook".equals( parts[2] ) &&
              "notesAndFeedback".equals( parts[3] )
         )
      {
        // notes and feedback...
        if ( parts.length >= 6 && 
                ( "attempt".equals( parts[4] ) ||
                  "attemptNotes".equals( parts[4] )
                )
           )
        {
          String attemptid = parts[5];
          String outcomeid = getGeneralMap().get( "attempt->outcome", attemptid );
          String memberid  = getGeneralMap().get( "outcome->member", outcomeid );
          // annotate the file node with student's IDs
          addUserToPlan( node, memberid );
        }
        else if ( parts.length >= 6 && 
                    ( "gradeHistory".equals( parts[4] ) ||
                      "gradeHistoryNotes".equals( parts[4] ) ||
                      "gradeDetail".equals( parts[4] ) ||
                      "gradeDetailNotes".equals( parts[4] )
                    )
                )
        {
          String outcomeid = parts[5];
          String memberid  = getGeneralMap().get( "outcome->member", outcomeid );
          // annotate the file node with student's IDs
          addUserToPlan( node, memberid );
        }
      }      
    }
    
    void processResourceFile( ReaderNodeContext node, String[] parts  )
    {
      if ( parts.length < 2 )
        return;
      
      String base = parts[0];
      String type = getGeneralMap().get( "base->resourcetype", base );
      if ( type == null )
        return;
      
      switch ( type )
      {
        case "course/x-bb-attemptfiles" ->         processAttemptFile( node, parts );
        case "course/x-bb-attemptembeddedfiles" -> processAttemptEmbeddedFile( node, parts );
        case "resource/x-bb-wiki" ->               processWikiFile( node, parts );
        default -> node.setWritePlan( null );
      }
    }

    void processAttemptFile( ReaderNodeContext node, String[] parts  )
    {
      if ( parts.length < 2 )
        return;
      
      String attemptfileid = parts[1];
      String attemptid = getGeneralMap().get( "attemptfile->attempt", attemptfileid );
      String outcomeid = getGeneralMap().get( "attempt->outcome", attemptid );
      String memberid  = getGeneralMap().get( "outcome->member", outcomeid );
      // annotate the file node with student's IDs
      addUserToPlan( node, memberid );
    }

    void processAttemptEmbeddedFile( ReaderNodeContext node, String[] parts  )
    {
      String attemptid = parts[1];
      String outcomeid = getGeneralMap().get( "attempt->outcome", attemptid );
      String memberid  = getGeneralMap().get( "outcome->member", outcomeid );
      // annotate the file node with student's IDs
      addUserToPlan( node, memberid );
    }
    
    void processWikiFile( ReaderNodeContext node, String[] parts  )
    {
      // Not handled yet so leave out in case student data in there
      node.setWritePlan( null );
    }
  }

  class ProcessOther implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();

      // Node mode (specifically no 'omit' mode) so copy out
      // the current element.
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
    }
  }
  
  void addUserToPlan( ReaderNodeContext node, String memberid )
  {
    String userid = null, sid = null;
    if ( memberid != null )
      userid = getGeneralMap().get( "member->user", memberid );
    if ( userid != null )
      sid = getGeneralMap().get( "user->sid", userid );
    
    ElementWritePlan plan = node.getWritePlan();
    if ( plan == null )
    {
      plan = new ElementWritePlan( node.getStartElement() );
      node.setWritePlan( plan );
    }
    if ( memberid != null )
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVMEMBERIDATTRIBUTE, memberid ) );
    if ( userid != null )
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVUSERIDATTRIBUTE, userid ) );
    if ( sid != null )
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVSTUDENTIDATTRIBUTE, sid ) );
  }
}
