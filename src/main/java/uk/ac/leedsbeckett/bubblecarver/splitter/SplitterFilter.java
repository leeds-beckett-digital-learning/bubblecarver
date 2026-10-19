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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    new FilterMatcherImpl( "/archive",                     new ProcessArchive()      ),
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

  class ProcessArchive implements StartElementHandler
  {
    @Override
    public void handleStartElement() throws XMLStreamException, IOException
    {
      ReaderNodeContext node = getContext().getReaderNodeContext();
      String wantedsid = getContext().getFilterContext().studentId;
      ElementWritePlan plan = new ElementWritePlan( node.getStartElement() );
      plan.getAttributeList().add( new AttributeImpl( Names.BCARVSTUDENTIDATTRIBUTE, wantedsid ) );
      node.setWritePlan( plan );
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
      String gid = node.getAttributeValue( Names.BCARVGROUPIDATTRIBUTE );
      String path = node.getAttributeValue( Names.PATHATTRIBUTE );
      
      if ( 
              ( sid != null && sid.length() > 0 && 
                wantedsid.equals( sid ) )
           ||
              ( gid != null && 
                gid.length() > 0 && 
                getContext().getFilterContext().groupMap.isMemberOfGroup( gid, wantedsid ) )
         )
      {
        node.setWritePlan( new ElementWritePlan( node.getStartElement() ) );
        // And copy the actual file from the archive to the student directory...
        Path source = getContext().getFilterContext().archivePath.resolve( path );
        Path target = getContext().getFilterContext().studentPath.resolve( path );
        System.out.println( "Archive source : " + source.toString() );
        System.out.println( "Student target : " + target.toString() );
        // Only discard the source if this is not a group related file.
        // because it will be needed for another student.
        storeStudentFile( source, target, gid == null );
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
      String gid = node.getAttributeValue( Names.BCARVGROUPIDATTRIBUTE );

      if ( gid != null )
      {
        if ( getContext().getFilterContext().groupMap.isMemberOfGroup( gid, wantedsid ) )
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
      else if ( sid != null && sid.length() > 0 )
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
  
  void storeStudentFile( Path source, Path target ) throws IOException
  {
    storeStudentFile( source, target, true );
  }
  
  void storeStudentFile( Path source, Path target, boolean discardSource ) throws IOException
  {
    if ( !Files.exists( source ) )
    {
      System.out.println( "=========================================" );
      System.out.println( "Supressing error for missing source file." );
      System.out.println( "=========================================" );
      return;
    }
    if ( Files.exists( target ) )
    {
      boolean skip = source.toFile().length() == target.toFile().length();      
      if ( !skip )
        // This assumes that unzipped copy has been given correct last modified data
        // by 7za.exe and that the existing copy in the student dir has preserved
        // the attributes.
        skip = source.toFile().lastModified() == target.toFile().lastModified();
      if ( skip )
      {
        System.out.println( "Skipping file copy!" );
        return;        
      }
    }
      
    if ( !Files.exists( target.getParent() ) )
      Files.createDirectories( target.getParent() );

    Path moveaside=null;
    if ( Files.exists( target ) )
    {
      moveaside = target.resolveSibling( target.getFileName().toString() + ".saved" );
      Files.move( target, moveaside, StandardCopyOption.ATOMIC_MOVE );
    }
    
    boolean success = true;
    try
    {
      boolean moved = false;
      if ( discardSource )
      {
        // Don't want to keep source so try moving which will be
        // much faster.
        try
        {
          Files.move( source, target, StandardCopyOption.ATOMIC_MOVE );
          moved = true;
        }
        catch ( AtomicMoveNotSupportedException moveex )
        {
          // If destination dir is on different file system
          moved = false;
        }
      }

      // move failed or not attempted so try copying
      if ( !moved )
      {
        long size = Files.size( source );
        try ( InputStream in = Files.newInputStream( source );
              OutputStream out = Files.newOutputStream( target )  )
        {
          if ( size < (2L*1024L*1024L*1024L) )
            IOUtils.copy( in, out );
          else
            IOUtils.copyLarge( in, out );
        }
        target.toFile().setLastModified( source.toFile().lastModified() ); 
      }
        
    }
    catch ( Exception e )
    {
      success = false;
      throw e;
    }
    finally
    {
      if ( !success )
      {
        Files.deleteIfExists( target );
        if ( moveaside != null )
          Files.move( moveaside, target, StandardCopyOption.ATOMIC_MOVE );      
      }
    }
    
    if ( success && discardSource )
      // won't need to delete if it was moved
      Files.deleteIfExists( source );
  }
}
