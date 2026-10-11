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
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION",   new ProcessOutcomeDefinition() ),
    new FilterMatcherImpl( "GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME",
            new ProcessOutcome() ),
    new FilterMatcherImpl( "GRADEBOOK/GRADE_HISTORY_ENTRIES/GRADE_HISTORY_ENTRY",
            new ProcessHistoryEntry() ),
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
      node.setWritePlan( addUserToPlan( node, memberid ) );
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
      node.setWritePlan( addUserToPlan( node, memberid ) );
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
  
  ElementWritePlan addUserToPlan( ReaderNodeContext node, String memberid )
  {
    String userid = null, sid = null;
    if ( memberid != null )
      userid = getGeneralMap().get( "member->user", memberid );
    if ( userid != null )
      sid = getGeneralMap().get( "user->sid", userid );
    
    ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
    if ( memberid != null )
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVMEMBERIDATTRIBUTE, memberid ) );
    if ( userid != null )
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVUSERIDATTRIBUTE, userid ) );
    if ( sid != null )
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVSTUDENTIDATTRIBUTE, sid ) );
    return plan;
  }
}
