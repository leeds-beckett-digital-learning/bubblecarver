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
public class GroupAttemptEmbeddedFilesFilter extends CombinerFilter
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "/GROUPATTEMPTEMBEDDEDFILES/GROUPATTEMPTEMBEDDEDFILE", new ProcessGAEFile() ),
    new FilterMatcherImpl( "/GROUPATTEMPTEMBEDDEDFILES/GROUPATTEMPTEMBEDDEDFILE/ATTEMPTID", new ProcessAttemptId() ),
    new FilterMatcherImpl( new ProcessOther() )          
  };

  public GroupAttemptEmbeddedFilesFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config )
  {
    super( reader, writer, context, config );
    setMatchers( matchers );
  }

  class ProcessGAEFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String bcarvid = getNextBackreferenceId();
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVIDATTRIBUTE, bcarvid ));
      node.setWritePlan( plan );
    }
  }

  class ProcessAttemptId implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node   = getContext().getReaderNodeContext();
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      String attemptid         = node.getAttributeValue( Names.VALUEATTRIBUTE );
      String bcarvid           = parent.getWrittenPlan().getAttributeValue( Names.BCARVIDATTRIBUTE );
      // The element is ATTEMPTID but really reference to group attempt ID.
      getGeneralMap().put( "bcarvid->groupattempt", bcarvid, attemptid );
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
}
