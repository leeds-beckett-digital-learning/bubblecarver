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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import org.apache.commons.lang3.StringUtils;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;
import uk.ac.leedsbeckett.bubblecarver.xsl.DateProcessor;

/**
 *
 * @author maber01
 */
public class SecondPassFilter extends CombinerFilter
{
  // Is thread safe for creating Matchers
  static Pattern timestampPattern = Pattern.compile( "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} [A-Z0-9]{3}" );
  // Can be reused by resetting but is not thread safe
  Matcher matcher = timestampPattern.matcher("");
  // Avoid too much init of arrays by using this one for
  // every attribute checked Must remember to keep clearing it.
  final ArrayList<Attribute> extraAttributes = new ArrayList<>();
  
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

  /**
   * No matter what part of this processor decides to output an
   * element here it can be altered before output.  This is used to
   * detect attributes with timestamps and add an attribute with
   * the timestamp converted to an integer number.  This will allow
   * later processing to more easily sort elements. (Especially
   * XPath 1.0 which can't sort strings.)
   * 
   * @param plan
   * @throws XMLStreamException 
   */
  @Override
  public void writeStartElement(ElementWritePlan plan) throws XMLStreamException
  {
    extraAttributes.clear();
    for ( Attribute a : plan.getAttributeList() )
    {
      QName aName = a.getName();
      String value = a.getValue();
      // Only interested in empty namespace attributes
      if ( !"".equals( aName.getNamespaceURI() ) )
        continue;
      
      // Use regular expression to detect date time in hope
      // it will be faster than catching failed datetime parse.
      // Reset the instance matcher with the source string
      matcher.reset( value );
      if ( matcher.matches() )
      {
        // The entire attribute value is a date/time stamp in the
        // format we are interested in.
        // Now try parsing as datetime and convert to one number
        String epoch = DateProcessor.epoch( value );
        // Make attribute in special name space but with name name.
        QName iaName = new QName( Names.INSTANTNAMESPACE, aName.getLocalPart() );
        Attribute instantAttribute = new AttributeImpl( iaName, epoch );
        extraAttributes.add( instantAttribute );
        
//        // For testing convert back to date/time
//        String reverse = DateProcessor.reverse( epoch );
//        QName revName = new QName( Names.INSTANTNAMESPACE, "x_" + aName.getLocalPart() );
//        Attribute reverseAttribute = new AttributeImpl( revName, reverse );
//        extraAttributes.add( reverseAttribute );
//
//        String utc = DateProcessor.utc( epoch );
//        QName uName = new QName( Names.INSTANTNAMESPACE, "u_" + aName.getLocalPart() );
//        Attribute uAttribute = new AttributeImpl( uName, utc );
//        extraAttributes.add( uAttribute );
      }
    }

    // Added after because we can't iterate the plan's list and
    // add to it at the same time.
    plan.getAttributeList().addAll( extraAttributes );
    // Empty this ready for next usage
    extraAttributes.clear();
    
    // Now let the super class actually output it.
    super.writeStartElement(plan);
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
      {
        // For now silently remove the rubric evaluation element
        // and all children elements.
        // Throwing an exception here helped find an example module to help
        // work out how to handle properly:
        // Z:\transfers\testdata\rubric\ArchiveFile_14556-2010.zip
        // Maybe not a good example - seems that rubric wasn't used.
        // Need to be processing individual responses to tests before
        // tackling this.
        node.setWritePlan( null );
        node.setMode( Mode.OMIT );
      }
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
