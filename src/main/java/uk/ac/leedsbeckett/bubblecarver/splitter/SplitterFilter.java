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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.IOUtils;
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
    new FilterMatcherImpl( "/archive/catalogue/file",      new ProcessFile()         ),
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

  class ProcessFile implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException, IOException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String wantedsid = getContext().getFilterContext().studentId;
      String sid = node.getAttributeValue( Names.BCARVSTUDENTIDATTRIBUTE );
      String path = node.getAttributeValue( Names.PATHATTRIBUTE );
      
      if ( sid != null && sid.length() > 0 && wantedsid.equals( sid ) )
      {
        node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
        // And copy the actual file from the archive to the student directory...
        Path source = getContext().getFilterContext().archivePath.resolve( path );
        Path target = getContext().getFilterContext().studentPath.resolve( path );
        System.out.println( "Archive source : " + source.toString() );
        System.out.println( "Student target : " + target.toString() );
        copyFileIfNecessary( source, target );
      }
      else
        node.setWritePlan( null );
      // no point setting mode because this node doesn't have any children
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
  
  void copyFileIfNecessary( Path source, Path target ) throws IOException
  {
    boolean necessary = true;
    if ( Files.exists( target ) )
    {
      FileTime sourcetime = Files.getLastModifiedTime( source );
      FileTime targettime = Files.getLastModifiedTime( target );
      necessary = sourcetime.compareTo( targettime ) > 0;
    }
    if ( necessary )
    {
      if ( !Files.exists( target.getParent() ) )
        Files.createDirectories( target.getParent() );
      long size = Files.size( source );
      try ( InputStream in = Files.newInputStream( source );
            OutputStream out = Files.newOutputStream( target )  )
      {
        if ( size < (2L*1024L*1024L*1024L) )
          IOUtils.copy( in, out );
        else
          IOUtils.copyLarge( in, out );
      }
    }
    else
      System.out.println( "Skipping file copy!" );
  }
}
