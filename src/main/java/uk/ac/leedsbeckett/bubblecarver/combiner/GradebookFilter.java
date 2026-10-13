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
import javax.xml.stream.events.Attribute;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class GradebookFilter extends CombinerFilter
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION",            new ProcessOutcomeDefinition() ),
    new FilterMatcherImpl( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/CONTENTID",  new ProcessContentId() ),
    new FilterMatcherImpl( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/COURSEMEMBERSHIPID",
            new ProcessMembershipId() ),
    new FilterMatcherImpl( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT",
            new ProcessAttempt() ),
    new FilterMatcherImpl( "/GRADEBOOK/GRADE_HISTORY_ENTRIES/GRADE_HISTORY_ENTRY/USERID",
            new ProcessHistoryUserId() ),

    new FilterMatcherImpl( new ProcessOther() )          
  };

  public GradebookFilter(
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
      // Clone the incoming element
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      node.setWritePlan( plan );
    }
  }
  
  class ProcessContentId implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node   = getContext().getReaderNodeContext();
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      ElementWritePlan parentplan = parent.getWrittenPlan();
      // get the backreference ID from before
      String outdefid = parentplan.getAttributeValue( Names.IDATTRIBUTE );
      
      String contentid = node.getStartElement().getAttributeByName( Names.VALUEATTRIBUTE ).getValue();
      if ( !contentid.isEmpty() )
        getGeneralMap().put( "outcomedef->content", outdefid, contentid );
              
      // Clone the incoming element
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
    }
  }
  
  class ProcessMembershipId implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node   = getContext().getReaderNodeContext();
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      ElementWritePlan parentplan = parent.getWrittenPlan();
      // get the backreference ID from before
      String outcomeid = parentplan.getAttributeValue( Names.IDATTRIBUTE );
      
      String memberid = node.getStartElement().getAttributeByName( Names.VALUEATTRIBUTE ).getValue();
      if ( !memberid.isEmpty() )
        getGeneralMap().put( "outcome->member", outcomeid, memberid );
              
      // Clone the incoming element
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
    }
  }

  class ProcessAttempt implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext attempt   = getContext().getReaderNodeContext();
      ReaderNodeContext outcome = getContext().getReaderNodeContext( 2 );
      
      getGeneralMap().put( 
              "attempt->outcome", 
              attempt.getAttributeValue(Names.IDATTRIBUTE), 
              outcome.getAttributeValue(Names.IDATTRIBUTE) );
              
      // Clone the incoming element
      attempt.setWritePlan( new ElementWritePlan( attempt.getStartElement() ) );
    }
  }
  
  class ProcessHistoryUserId implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext userid  = getContext().getReaderNodeContext();
      ReaderNodeContext entry = getContext().getReaderNodeContext( 1 );
      
      getGeneralMap().put( 
              "gradehistoryentry->user", 
              entry.getAttributeValue(Names.IDATTRIBUTE), 
              userid.getAttributeValue(Names.VALUEATTRIBUTE) );
              
      // Clone the incoming element
      userid.setWritePlan( new ElementWritePlan( userid.getStartElement() ) );
    }
  }
  
  class ProcessOther implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
    }
  }

}
