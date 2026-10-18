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
package uk.ac.leedsbeckett.bubblecarver.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.combiner.Names;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class ManifestFilter extends StaxFilter<BubbleAnalyserContext,ReaderNodeContext,WriterNodeContext>
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( "/manifest/resources/resource",      new ProcessResource() ),
    new FilterMatcherImpl(                                      new ProcessOther() )          
  };
  
  final boolean dummyRun;
  final ArrayList<Path> wantedFiles;
  
  public ManifestFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleAnalyserContext, ReaderNodeContext, WriterNodeContext> context, 
          StaxFilterConfiguration config,
          boolean dummyRun )
  {
    super( reader, writer, context, config );
    this.dummyRun = dummyRun;
    wantedFiles = dummyRun ? new ArrayList<>() : null;
    setMatchers( matchers );
  }

  public ArrayList<Path> getWantedFiles()
  {
    return wantedFiles;
  }

  class ProcessResource implements StartElementHandler
  {
    ReaderNodeContext node;
    String resourceType;
    
    @Override
    public void handleStartElement() throws XMLStreamException, IOException
    {
      node = getContext().getReaderNodeContext();
      String fileref = node.getStartElement().getAttributeByName( Names.BBFILEATTRIBUTE ).getValue();
      Path sourcePath = getContext().getFilterContext().inputBasePath.resolve( fileref );
      resourceType = node.getStartElement().getAttributeByName( Names.TYPEATTRIBUTE ).getValue();
      if ( resourceType == null ) resourceType = "";
      
      if ( dummyRun )
      {
        node.setWritePlan( null );
        if ( "resource/x-bb-document".equals( resourceType ) || 
             "resource/x-bb-blti-link".equals( resourceType )   )
          wantedFiles.add( sourcePath );
        return;
      }

      String dochandler = null;
      String imsurl = null;
      if ( "resource/x-bb-document".equals( resourceType ) || 
           "resource/x-bb-blti-link".equals( resourceType )   )
      {
        try ( InputStream in = Files.newInputStream( sourcePath ) )
        {
          getConfig().outputFragment = true;
          // Create a context with a copy of the general context but
          // fresh reader and writer contexts.
          FullFilterContext<BubbleAnalyserContext,ReaderNodeContext,WriterNodeContext> ffc 
                  = new FullFilterContext<>( getContext().getFilterContext() );
          XMLEventReader reader = getContext()
                                      .getFilterContext()
                                      .getReaderFactory()
                                      .createXMLEventReader( in );
          try
          {
            switch ( resourceType )
            {
              case "resource/x-bb-document" ->
              {
                DocumentFilter docfilter = new DocumentFilter( reader, getWriter(), ffc, getConfig() );
                docfilter.process();
                dochandler = docfilter.getHandler();
              }
              case "resource/x-bb-blti-link" ->
              {
                ImsLinkFilter imsfilter = new ImsLinkFilter( reader, getWriter(), ffc, getConfig() );
                imsfilter.process();
                imsurl = imsfilter.getUrl();
              }
            }
          }
          catch ( XMLStreamException ex )
          {
            ex.printStackTrace();
            // Plough on....
          }
        }
      }
      
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      plan.getAttributeList().clear();
      plan.getAttributeList().add( new AttributeImpl( Names.TYPEATTRIBUTE, resourceType ) );      
      if ( dochandler != null )
        plan.getAttributeList().add( new AttributeImpl( Names.HANDLERATTRIBUTE, dochandler ) );
      if ( imsurl != null )
        plan.getAttributeList().add( new AttributeImpl( Names.IMSURLATTRIBUTE, imsurl ) );
      node.setWritePlan( plan );
    }
  }
  
  class ProcessOther implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      node.setWritePlan( null );
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
