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
import java.util.ArrayList;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.data.WantedManifestResourceTypes;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;
import uk.ac.leedsbeckett.bubblecarver.zip.ZipProcessor;

/**
 *
 * @author maber01
 */
public class ManifestFilter extends CombinerFilter
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
  
  final boolean dummyRun;
  final ArrayList<Path> wantedFiles;
  
  public ManifestFilter(
          XMLEventReader reader, 
          XMLStreamWriter writer, 
          FullFilterContext<BubbleCombinerContext, ReaderNodeContext, WriterNodeContext> context, 
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
      
      if ( dummyRun )
      {
        // The resource file itself
        wantedFiles.add( sourcePath );
      }
      else
      {
        if ( WantedManifestResourceTypes.isWeird( resourceType ) )
          processWeirdResource();
        else
          processNormalResource( sourcePath );
      }
      
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
          case "membership/x-bb-groupmembership" ->
            subfilter = new GroupMembershipFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-gradebook" ->
            subfilter = new GradebookFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-groupupload" ->
            subfilter = new GroupUploadFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-attemptfiles" ->
            subfilter = new AttemptFilesFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-attemptembeddedfiles" ->
            // This is probably not needed? because there is no attempt file ID in the resource to record.
            subfilter = new AttemptEmbedFilesFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-groupattemptfiles" ->
            subfilter = new GroupAttemptFilesFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-groupattemptembeddedfiles" ->
            subfilter = new GroupAttemptEmbeddedFilesFilter( reader, getWriter(), ffc, getConfig() );
          case "course/x-bb-coursesetting" ->
            // Currently not handled in a special way - maybe dates are of interest.
            subfilter = new CourseSettingFilter( reader, getWriter(), ffc, getConfig() );
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
      // do nothing with the resource itself - leave it empty
      
      // qti-attempts have a plain text (not xml) nothing to see here
      // resource - all relevent data is in the associated resource directory
      // This will make it difficult for the second pass filter to
      // mark up those files.
      
      // Most availability rule resources are NOT of interest. However,
      // from time to time they refer to achievements and these contain
      // important information about students.  The availability is the
      // only way to work out which students own which acheivments.
    }
  }
  
  
  /**
   * Will process files for 'weird' resources types too
   */
  class ProcessResourceFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException, IOException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      getContext().getReaderNodeContext().setWritePlan( new ElementWritePlan( node.getStartElement() ) );      

      if ( dummyRun )
      {
        ReaderNodeContext parent = getContext().getReaderNodeContext( 1 );      
        String base = parent.getStartElement().getAttributeByName( Names.XMLBASEATTRIBUTE ).getValue();
        String href = node.getAttributeValue( Names.HREFATTRIBUTE );

        Path sourcePath = getContext().getFilterContext().inputBasePath.resolve( base ).resolve( href );
        System.out.println( "Processing resource FILE at " + sourcePath.toString() );
        wantedFiles.add( sourcePath );
      }      
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
