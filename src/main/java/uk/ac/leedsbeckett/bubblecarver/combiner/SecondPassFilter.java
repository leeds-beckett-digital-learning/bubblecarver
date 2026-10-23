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
import org.apache.commons.lang3.StringUtils;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class SecondPassFilter extends CombinerFilter
{
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( Mode.OMIT,                                                         new ProcessOmitted() ),
    new FilterMatcherImpl( "USERS/USER",                                                      new ProcessUser() ),
    new FilterMatcherImpl( "COURSEMEMBERSHIPS/COURSEMEMBERSHIP",                              new ProcessMembership() ),
    new FilterMatcherImpl( "GROUPS/GROUP",                                                    new ProcessGroup() ),
    new FilterMatcherImpl( "GROUPMEMBERSHIPS/GROUPMEMBERSHIP",                                new ProcessGroupMembership() ),
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION",                  new ProcessOutcomeDefinition() ),
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME", new ProcessOutcome() ),
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/GROUPATTEMPTS/GROUPATTEMPT", new ProcessGroupAttempt() ),
    new FilterMatcherImpl( "GRADEBOOK/GRADE_HISTORY_ENTRIES/GRADE_HISTORY_ENTRY",             new ProcessHistoryEntry() ),
    new FilterMatcherImpl( "GROUPUPLOADS/UPLOAD",                                             new ProcessGroupUpload() ),
    new FilterMatcherImpl( "GROUPATTEMPTFILES/GROUPATTEMPTFILE",                              new ProcessGroupAttemptFile() ),
    new FilterMatcherImpl( "GROUPATTEMPTEMBEDDEDFILES/GROUPATTEMPTEMBEDDEDFILE",              new ProcessGroupAttemptEmbeddedFile() ),
    new FilterMatcherImpl( "RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION",                  new ProcessRubricEvaluation() ),
    new FilterMatcherImpl( "/archive/manifest/resources/resource/file",                       new ProcessResourceFile() ),
    new FilterMatcherImpl( "/archive/catalogue/file",                                         new ProcessCatalogueFile() ),
    new FilterMatcherImpl(                                                                    new ProcessOther() )          
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
  
  class ProcessGroup implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String groupid = node.getAttributeValue( Names.IDATTRIBUTE );
      addGroupToPlan( node, groupid );
    }
  }
  
  class ProcessGroupMembership implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String groupmemberid = node.getAttributeValue( Names.IDATTRIBUTE );
      String gid = getContext().getFilterContext().getGroupMap().getGroupOfMember( groupmemberid );
      addGroupToPlan( node, gid );
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

  class ProcessGroupAttempt implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String groupattemptid = node.getAttributeValue( Names.IDATTRIBUTE );
      String groupid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      addGroupToPlan( node, groupid );
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

  class ProcessGroupUpload implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String uploadid = node.getAttributeValue( Names.IDATTRIBUTE );
      String groupid = getGeneralMap().get( "groupupload->group", uploadid );
      addGroupToPlan( node, groupid );
    }
  }

  class ProcessGroupAttemptFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String groupattemptfileid = node.getAttributeValue( Names.IDATTRIBUTE );
      String groupattemptid = getGeneralMap().get( "groupattemptfile->groupattempt", groupattemptfileid );
      String gid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      addGroupToPlan( node, gid );
    }
  }
  
  class ProcessGroupAttemptEmbeddedFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String bbcarvid = node.getAttributeValue( Names.BCARVIDATTRIBUTE );
      // Although the element is "ATTEMPTID" this is really a reference to a group attempt
      String groupattemptid = getGeneralMap().get( "bcarvid->groupattempt", bbcarvid );
      String gid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      addGroupToPlan( node, gid );
    }
  }
  
  class ProcessRubricEvaluation implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      String userid = null;
      String memberid = null;
      String gid = null;
      String outcomeid = null;
      
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String rubevalid = node.getAttributeValue( Names.IDATTRIBUTE );

      // One only of these will be non-empty
      String gradeid          = getGeneralMap().get( "rubriceval->grade", rubevalid );
      String attemptid        = getGeneralMap().get( "rubriceval->attempt", rubevalid );
      String groupattemptid   = getGeneralMap().get( "rubriceval->groupattempt", rubevalid );
      String qtidataid        = getGeneralMap().get( "rubriceval->qtidata", rubevalid );
      String gradehistoryid   = getGeneralMap().get( "rubriceval->gradehistory", rubevalid );
      String stagedattemptid  = getGeneralMap().get( "rubriceval->stagedattempt", rubevalid );
      String stagedgrpattid   = getGeneralMap().get( "rubriceval->stagedgroupattempt", rubevalid );
      
      if ( !StringUtils.isBlank( gradehistoryid ) )
      {
        userid = getGeneralMap().get( "gradehistoryentry->user", gradehistoryid );
        memberid = getGeneralMap().get( "user->member", userid );
        if ( memberid == null )
        {
          // If there is a grade history entry for a user who has been
          // removed from the course, redact the entry or leave it in?
          // Leave in but remember to redact to student specific data.
          node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
          return;
        }
      }
      else if ( !StringUtils.isBlank( attemptid ) )
      {
        outcomeid = getGeneralMap().get( "attempt->outcome", attemptid );
        memberid  = getGeneralMap().get( "outcome->member", outcomeid );
      }
      else if ( !StringUtils.isBlank( stagedattemptid ) )
      {
        outcomeid = getGeneralMap().get( "stagedattempt->outcome", stagedattemptid );
        memberid  = getGeneralMap().get( "outcome->member", outcomeid );
      }
      else if ( !StringUtils.isBlank( groupattemptid ) )
      {
        gid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      }
      else if ( !StringUtils.isBlank( stagedgrpattid ) )
      {
        groupattemptid = getGeneralMap().get( "stagedgroupattempt->groupattempt", stagedgrpattid );
        gid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      }
      else if ( !StringUtils.isBlank( gradeid ) )
        throw new IllegalArgumentException( "Cannot handle rubrics attached to a gradebook grade." );
      else if ( !StringUtils.isBlank( qtidataid ) )
        throw new IllegalArgumentException( "Cannot handle rubrics attached to QTI data." );
      else
        throw new IllegalArgumentException( "Rubric evaluation that doesn't seem to be attached to anything." );
      
//      if ( memberid == null && gid == null )
//        throw new IllegalArgumentException( "Unable to attribute rubric evaluation to a student or a group." );
        
      if ( memberid != null )
        addUserToPlan( node, memberid );
      
      if ( gid != null )
        addGroupToPlan( node, gid );
    }
  }
  
  class ProcessResourceFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      String base = parent.getAttributeValue( Names.XMLBASEATTRIBUTE );
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String href = node.getAttributeValue( Names.HREFATTRIBUTE );
      String fullhref = base + "/" + href;
      String[] parts = fullhref.split( "/" );

      processResourceFile( node, parts );
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
                ( "groupattempt".equals( parts[4] ) ||
                  "groupattemptNotes".equals( parts[4] )
                )
           )
        {
          String gattemptid = parts[5];
          String groupid = getGeneralMap().get( "groupattempt->group", gattemptid );
          // annotate the file node with student's IDs
          addGroupToPlan( node, groupid );
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
        case "course/x-bb-attemptfiles"              -> processAttemptFile( node, parts );
        case "course/x-bb-attemptembeddedfiles"      -> processAttemptEmbeddedFile( node, parts );
        case "course/x-bb-groupattemptfiles"         -> processGroupAttemptFile( node, parts );
        case "course/x-bb-groupattemptembeddedfiles" -> processGroupAttemptEmbeddedFile( node, parts );
        case "course/x-bb-groupupload"               -> processGroupUploadFile( node, parts );
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
    
    void processGroupAttemptFile( ReaderNodeContext node, String[] parts  )
    {
      if ( parts.length < 2 )
        return;
      
      String groupattemptfileid = parts[1];
      String groupattemptid = getGeneralMap().get( "groupattemptfile->groupattempt", groupattemptfileid );
      String gid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      // annotate the file node with student's IDs
      addGroupToPlan( node, gid );
    }

    void processGroupAttemptEmbeddedFile( ReaderNodeContext node, String[] parts  )
    {
      String groupattemptid = parts[1];
      String gid = getGeneralMap().get( "groupattempt->group", groupattemptid );
      // annotate the file node with student's IDs
      addGroupToPlan( node, gid );
    }
    
    void processGroupUploadFile( ReaderNodeContext node, String[] parts  )
    {
      if ( parts.length < 2 )
        return;
      String uploadid = parts[1];
      String groupid = getGeneralMap().get( "groupupload->group", uploadid );
      addGroupToPlan( node, groupid );
    }

    void processWikiFile( ReaderNodeContext node, String[] parts  )
    {
      // Not handled yet so leave out in case student data in there
      node.setWritePlan( null );
    }
    
  class ProcessOmitted implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      getContext().getReaderNodeContext().setWritePlan( null );
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

  void addGroupToPlan( ReaderNodeContext node, String groupid )
  {
    if ( groupid == null ) return;
    ElementWritePlan plan = node.getWritePlan();
    if ( plan == null )
    {
      plan = new ElementWritePlan( node.getStartElement() );
      node.setWritePlan( plan );
    }
    plan.getAttributeList().add( new AttributeImpl( Names.BCARVGROUPIDATTRIBUTE, groupid ) );
  }

}
