/*
 * Copyright 2025 maber01.
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
package uk.ac.leedsbeckett.bubblecarver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;

/**
 *
 * @author maber01
 */
public class BatchProcessor
{
  public static void main( String[] args )
  {
    Path in = new File("c:\\temp\\archives").toPath();
    Path out = new File("c:\\temp\\students").toPath();
    try
    {
      BatchProcessor bp = new BatchProcessor( in, out );
      bp.process();
    }
    catch ( Exception ex )
    {
      ex.printStackTrace();
    }
  }
  
  private final Path searchPath;
  private final Path studentPath;

  private final Transformer transformListStudents;
  private final Transformer transformStudentReport;
  
  ProcessProgress progress;
  
  public BatchProcessor( Path searchPath, Path studentPath ) throws IOException, TransformerConfigurationException
  {
    this.searchPath = searchPath;
    this.studentPath = studentPath;
    
    TransformerFactory tFactory = TransformerFactory.newInstance();
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/liststudents.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformListStudents = templates.newTransformer();
    }
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/studentreport.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformStudentReport = templates.newTransformer();
    }
  }
 
  public void process() throws IOException
  {
    progress = new ProcessProgress();
    UnzippedArchiveSearcher searcher = new UnzippedArchiveSearcher();
    Stream<Path> streamSearch = Files.find( searchPath, 2, searcher );
    streamSearch.forEach( new ArchiveProcessor() );
    if ( progress.firstException != null )
    {
      System.out.println( "Exception while processing stream of archives..." );
      progress.firstException.printStackTrace();
    }

    progress = new ProcessProgress();
    StudentArchiveDataSearcher sdsearcher = new StudentArchiveDataSearcher();
    Stream<Path> studentArchiveDataSearch = Files.find( studentPath, 10, sdsearcher );
    studentArchiveDataSearch.forEach( new StudentArchiveDataProcessor() );
    if ( progress.firstException != null )
    {
      System.out.println( "Exception while processing stream of archives..." );
      progress.firstException.printStackTrace();
    }    
  }
  
  public class ProcessProgress
  {
    public int completedCount=0;
    public Exception firstException = null;
  }
  
  public class ArchiveProcessor implements Consumer<Path>
  {
    @Override
    public void accept( Path t )
    {
      if ( progress.firstException != null )
        return;
      
      System.out.println( "Processing " + t.toUri() );
      FileCataloguer fc = new FileCataloguer( t );
      Document catalogue = null;
      try
      {
        catalogue = fc.getCatalogue();
      }
      catch ( ParserConfigurationException | IOException ex )
      {
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      
      Resolver resolver = new Resolver( t, catalogue );
      
      try ( InputStream in = new FileInputStream( t.resolve( "imsmanifest.xml" ).toFile() ) )
      {
        StreamSource source = new StreamSource( in );
        StringBufferResult result = createStringBufferResult();
        transformListStudents.reset();
        transformListStudents.setParameter( "output-base-uri", studentPath.toString() );
        transformListStudents.setURIResolver( resolver );
        transformListStudents.transform( source, result );
        System.out.println( result.toString() );
      }
      catch ( IOException | TransformerException ex )
      {
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      progress.completedCount++;
    }
  }

  public class StudentArchiveDataProcessor implements Consumer<Path>
  {
    @Override
    public void accept( Path t )
    {
      if ( progress.firstException != null )
        return;
      
      System.out.println( "Processing " + t.toUri() );

      Resolver resolver = new Resolver( t, null );

      Path report = t.getParent().resolve( "report.html" );
      try ( InputStream in = new FileInputStream( t.toFile() );
            OutputStream out = new FileOutputStream( report.toFile() ) )
      {
        StreamSource source = new StreamSource( in );
        StreamResult result = new StreamResult( out );
        transformStudentReport.reset();
        transformStudentReport.setURIResolver( resolver );
        transformStudentReport.transform( source, result );
      }
      catch ( IOException | TransformerException ex )
      {
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      progress.completedCount++;
    }
  }
  
  public class UnzippedArchiveSearcher implements BiPredicate<Path,BasicFileAttributes>
  {
    @Override
    public boolean test( Path t, BasicFileAttributes u )
    {
      if ( !u.isDirectory() )
        return false;
      if ( !t.getFileName().toString().endsWith( ".zip") )
        return false;
      Path im = t.resolve( "imsmanifest.xml" );
      return Files.exists( im );
    }
  }  
  
  public class StudentArchiveDataSearcher implements BiPredicate<Path,BasicFileAttributes>
  {
    @Override
    public boolean test( Path t, BasicFileAttributes u )
    {
      if ( !u.isRegularFile() )
        return false;
      if ( !t.getFileName().toString().equals( "data.xml") )
        return false;
      Path relpath = studentPath.relativize( t );
      if ( relpath.getNameCount() != 7 ) return false;
      if ( !"bbarchives".equals( relpath.getName( 4 ).toString() ) ) return false;
      Path reppath = t.getParent().resolve( "report.html" );
      if ( !Files.exists( reppath ) ) return true;
      try
      {
        BasicFileAttributes repatt = Files.readAttributes( reppath, BasicFileAttributes.class );
        if ( repatt.lastModifiedTime().compareTo( u.lastModifiedTime() ) < 0 )
          return true;
      }
      catch ( IOException ex )
      {
      }
      return false;
    }
  }  
  
  public StringBufferResult createStringBufferResult()
  {
    return new StringBufferResult( new StringWriter() );
  }
  
  public class StringBufferResult extends StreamResult
  {
    final StringWriter writer;
    public StringBufferResult( StringWriter writer )
    {
      super( writer );
      this.writer = writer;
    }
    
    @Override
    public String toString()
    {
      return writer.toString();
    }
  }
  
  public class Resolver implements URIResolver
  {
    final Path archiveBase;
    final Document catalogue;

    public Resolver( Path archiveBase, Document catalogue )
    {
      this.archiveBase = archiveBase;
      this.catalogue = catalogue;
    }
    
    @Override
    public Source resolve( String href, String base ) throws TransformerException
    {
      //System.out.println( "Resolving [" + href + "], [" + base + "]" );
      if ( href.equals( "catalogue:" ) )
        return new DOMSource( catalogue );
      return new StreamSource( archiveBase.resolve( href ).toFile() );
    }
    
  }
}
