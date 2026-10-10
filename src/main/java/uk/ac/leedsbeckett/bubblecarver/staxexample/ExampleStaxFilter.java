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
package uk.ac.leedsbeckett.bubblecarver.staxexample;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.stax.MatchProcessor;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class ExampleStaxFilter extends StaxFilter<FilterContext,ReaderNodeContext,WriterNodeContext>
{
  enum Mode implements FilterMode
  {
    OMIT( "omit" ),
    OTHER( "other" );
    
    private final String name;
    Mode( String name ) { this.name = name; }
    @Override
    public String getName() { return name; }
  }
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( Mode.OMIT,                                           new ProcessOmitted() ),
    new FilterMatcherImpl(                      "/manifest/organizations",      new ProcessOrganizations() ),
    new FilterMatcherImpl(                                                      new ProcessOther() )
          
  };

  public ExampleStaxFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<FilterContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config )
  {
    super( reader, writer, context, config );
    setMatchers( matchers );
  }

  class ProcessOmitted implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      getContext().getReaderNodeContext().setWritePlan( null );
    }    
  }
  
  class ProcessOrganizations implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      getContext().getReaderNodeContext().setWritePlan( null );
      ReaderNodeContext node = getContext().getReaderNodeContext();

      // Output this element
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
      // But set a mode that will omit child elements (not child character blocks though)
      node.setMode( Mode.OMIT );
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
