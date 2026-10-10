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
public class GradebookFilter extends StaxFilter<BubbleCombinerContext,ReaderNodeContext,WriterNodeContext>
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION",            new ProcessOutcomeDefinition() ),
    new FilterMatcherImpl( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/CONTENTID",  new ProcessContentId() ),
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
      getWriter().writeCharacters( "\n" );
      ReaderNodeContext node = getContext().getReaderNodeContext();
      // Clone the incoming element
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      String bcid = getContext().getFilterContext().getNextBackreferenceId();
//      // add an new unique ID to it
      
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVIDATTRIBUTE, bcid ) );
      node.setWritePlan( plan );
      
      // Analyse attributes and make records.
    }
  }
  
  class ProcessContentId implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      getWriter().writeCharacters( "\n" );
      ReaderNodeContext node   = getContext().getReaderNodeContext();
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      ElementWritePlan parentplan = parent.getWrittenPlan();
      // get the backreference ID from before
      String bcid = parentplan.getAttributeValue( Names.BCARVIDATTRIBUTE );
      
      String contentid = node.getStartElement().getAttributeByName( Names.VALUEATTRIBUTE ).getValue();
      if ( !contentid.isEmpty() )
        getContext().getFilterContext().getGeneralMap().put( "bcarvid-content", bcid, contentid );
              
      // Clone the incoming element
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
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

  @Override
  public ReaderNodeContext createReaderNode()
  {
    return new ReaderNodeContext();
  }

  @Override
  public WriterNodeContext createWriterNode()
  {
    return new WriterNodeContext();
  }
}
