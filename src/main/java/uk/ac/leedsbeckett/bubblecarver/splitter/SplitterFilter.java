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
package uk.ac.leedsbeckett.bubblecarver.splitter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.combiner.Names;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class SplitterFilter extends StaxFilter<BubbleSplitterContext,ReaderNodeContext,WriterNodeContext>
{
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( Mode.WRONGSTUDENT,              new ProcessWrongStudent() ),
    new FilterMatcherImpl( Mode.RIGHTSTUDENT,              new ProcessRightStudent() ),
    new FilterMatcherImpl(                                 new ProcessOther()        )          
  };

  public SplitterFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleSplitterContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config )
  {
    super( reader, writer, context, config );
    setMatchers( matchers );
  }

  class ProcessWrongStudent implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      getContext().getReaderNodeContext().setWritePlan( null );
    }
  }

  class ProcessRightStudent implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
    }
  }

  class ProcessOther implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String wantedsid = getContext().getFilterContext().studentId;
      String sid = node.getAttributeValue( Names.BCARVSTUDENTIDATTRIBUTE );
      if ( sid != null && sid.length() > 0 )
      {
        if ( wantedsid.equals( sid ) )
        {
          // Clone wanted element
          node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
          // Don't check contained nodes
          node.setMode( Mode.RIGHTSTUDENT );
        }
        else
        {
          // Don't write this node
          node.setWritePlan( null );
          // Don't check contained nodes
          node.setMode( Mode.WRONGSTUDENT );          
        }
      }
      else
      {
        // Non student node to clone out and continue
        node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
      }
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
