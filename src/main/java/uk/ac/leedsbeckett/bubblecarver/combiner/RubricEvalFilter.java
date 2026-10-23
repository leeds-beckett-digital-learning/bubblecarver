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
public class RubricEvalFilter extends CombinerFilter
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/RUBRIC_ID",               new Mapper("rubriceval->rubric") ),
    // Only one of these elements will be non empty. Which one depends on where the 
    // rubric was applied.
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/GRADEBOOK_GRADE_ID",      new Mapper("rubriceval->grade") ),
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/ATTEMPT_ID",              new Mapper("rubriceval->attempt") ),
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/GROUP_ATTEMPT_ID",        new Mapper("rubriceval->groupattempt") ),
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/QTI_RESULT_DATA_ID",      new Mapper("rubriceval->qtidata") ),
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/GRADEBOOK_LOG_ID",        new Mapper("rubriceval->gradehistory") ),
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/STAGED_ATTEMPT_ID",       new Mapper("rubriceval->stagedattempt") ),
    new FilterMatcherImpl( "/RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION/STAGED_GROUP_ATTEMPT_ID", new Mapper("rubriceval->stagedgroupattempt") ),
    new FilterMatcherImpl( new ProcessOther() )
  };

  public RubricEvalFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config )
  {
    super( reader, writer, context, config );
    setMatchers( matchers );
  }

  class Mapper implements StartElementHandler
  {
    final String mapname;

    public Mapper(String mapname)
    {
      this.mapname = mapname;
    }
    
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );
      String rubevalid = parent.getAttributeValue( Names.IDATTRIBUTE );
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String id = node.getAttributeValue( Names.VALUEATTRIBUTE );
      if ( !StringUtils.isBlank( id ) )
        getGeneralMap().put( mapname, rubevalid, id );
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
