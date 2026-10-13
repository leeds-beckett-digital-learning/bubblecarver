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
public class AttemptFilesFilter extends CombinerFilter
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "/ATTEMPTFILES/ATTEMPTFILE/ATTEMPTID", new ProcessAttemptId() ),
    new FilterMatcherImpl( new ProcessOther() )          
  };

  public AttemptFilesFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config )
  {
    super( reader, writer, context, config );
    setMatchers( matchers );
  }

  class ProcessAttemptId implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      String attemptid = node.getAttributeValue( Names.VALUEATTRIBUTE );
      String attemptfileid = parent.getAttributeValue( Names.IDATTRIBUTE );
      
      // Record the one to one relationship both ways
      getGeneralMap().put( "attemptfile->attempt", attemptfileid, attemptid );

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
