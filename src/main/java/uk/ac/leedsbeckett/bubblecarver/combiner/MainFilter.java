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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.data.WantedManifestResourceTypes;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class MainFilter extends CombinerFilter
{
  
  FilterMatcherImpl[] matchers = 
  {
    new FilterMatcherImpl( Mode.OMIT,                                                new ProcessOmitted() ),
    new FilterMatcherImpl(                      "/manifest",                         new ProcessManifest() ),
    new FilterMatcherImpl( Mode.MANIFEST,       "/manifest/organizations",           new ProcessOrganizations() ),
    new FilterMatcherImpl( Mode.MANIFEST,       "/manifest/resources/resource",      new ProcessResource() ),
    new FilterMatcherImpl( Mode.MANIFEST,       "/manifest/resources/resource/file", new ProcessResourceFile() ),
    new FilterMatcherImpl(                                                           new ProcessOther() )          
  };

  CombinerFilter subfilter;
  
  public MainFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
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

  class ProcessManifest implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
    
      // Output this element in due course
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );

      // Set mode
      node.setMode( Mode.MANIFEST );
    }
  }

  class ProcessOrganizations implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
    
      // Allow standard processing to output this element in due course...
      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );

      // But set a mode that will omit child elements (not child character blocks though)
      node.setMode( Mode.OMIT );
    }
  }

  class ProcessResource implements StartElementHandler
  {
    ReaderNodeContext node;
    String resourceType;
    
    @Override
    public void handleStartElement() throws XMLStreamException, IOException
    {
      node = getContext().getReaderNodeContext();
      String base = node.getAttributeValue( Names.XMLBASEATTRIBUTE );
      resourceType = node.getStartElement().getAttributeByName( Names.TYPEATTRIBUTE ).getValue();
      if ( resourceType == null ) resourceType = "";
      getGeneralMap().put( "base->resourcetype", base, resourceType );
      
      if ( !WantedManifestResourceTypes.isWanted( resourceType ) )
      {
        node.setMode( Mode.OMIT );
        return;
      }

      
      String fileref = node.getStartElement().getAttributeByName( Names.BBFILEATTRIBUTE ).getValue();

      node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
      writeStartElement( node );

      getWriter().writeStartElement( "bbcarv", "data", Names.BCARVNAMESPACE );
      
      Path sourcePath = getContext().getFilterContext().inputBasePath.resolve( fileref );
      System.out.println( "Processing resource at " + sourcePath.toString() );
      System.out.println( "Type = " + resourceType );
      
      if ( WantedManifestResourceTypes.isWeird( resourceType ) )
        processWeirdResource();
      else
        processNormalResource( sourcePath );
      
      
      getWriter().writeEndElement();
    }
    
    public void processNormalResource( Path sourcePath ) throws XMLStreamException, IOException
    {
      try ( InputStream in = Files.newInputStream( sourcePath ) )
      {
        getConfig().outputFragment = true;
        // Create a context with a copy of the general context but
        // fresh reader and writer contexts.
        FullFilterContext<BubbleCombinerContext,ReaderNodeContext,WriterNodeContext> ffc 
                = new FullFilterContext<>( getContext().getFilterContext() );

        XMLEventReader reader = getContext()
                                    .getFilterContext()
                                    .getReaderFactory()
                                    .createXMLEventReader( in );

        switch ( resourceType )
        {
          case "course/x-bb-user" ->
            subfilter = new UsersFilter( reader, getWriter(), ffc, getConfig() );
          case "membership/x-bb-coursemembership" ->
            subfilter = new MembershipFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-gradebook" ->
            subfilter = new GradebookFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-attemptfiles" ->
            subfilter = new AttemptFilesFilter( reader, getWriter(), ffc, getConfig() );
          default -> 
            subfilter = new DefaultResourceFilter( reader, getWriter(), ffc, getConfig() );
        }
        
        try
        {
          subfilter.process();
        }
        catch ( XMLStreamException ex )
        {
          ex.printStackTrace();
          // Plough on....
        }
      }      
    }
    
    public void processWeirdResource() throws XMLStreamException, IOException
    {
      
    }
  }
  
  class ProcessResourceFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException, IOException
    {
      getContext().getReaderNodeContext().setWritePlan( null );
      
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String href = node.getAttributeValue( Names.HREFATTRIBUTE );

      getWriter().writeStartElement( "bbcarv", "file", Names.BCARVNAMESPACE );
      getWriter().writeAttribute( Names.HREFATTRIBUTE.getNamespaceURI(), Names.HREFATTRIBUTE.getLocalPart(), href );
      getWriter().writeEndElement();
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

}
