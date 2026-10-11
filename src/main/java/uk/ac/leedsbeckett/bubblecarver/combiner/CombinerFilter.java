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
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.FullFilterContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.ReaderNodeContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.StaxFilter;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.StaxFilterConfiguration;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.WriterNodeContext;

/**
 *
 * @author maber01
 */
public abstract class CombinerFilter extends StaxFilter<BubbleCombinerContext,ReaderNodeContext,WriterNodeContext>
{
  
  public CombinerFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config
        )
  {
    super(reader, writer, context, config);
  }
  
  public GeneralMap getGeneralMap()
  {
    return getContext().getFilterContext().getGeneralMap();
  }

  public String getNextBackreferenceId()
  {
    return getContext().getFilterContext().getNextBackreferenceId();
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
