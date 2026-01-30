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

import uk.ac.leedsbeckett.bubblecarver.data.ModuleUserInfo;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.namespace.NamespaceContext;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper;
import uk.ac.leedsbeckett.bubblecarver.xsl.DataLookups;
import uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor;
import uk.ac.leedsbeckett.bubblecarver.xsl.ModuleUserInfoMapper;

/**
 *
 * @author maber01
 */
public class BatchProcessor implements Runnable
{
  public static void main( String[] args )
  {
    //Path in  = Path.of( "z:\\blackboard_archives\\sisfeed_2021_2022_Part1" );
    Path in     = Path.of( "c:\\temp\\samplea" );
    Path inter  = Path.of( "c:\\temp\\temp" );
    Path out    = Path.of( "c:\\temp\\students" );
    try
    {
      BatchProcessor bp = new BatchProcessor( null, in, inter, out, true, true, false );
      bp.setForceReport( true );
      bp.processForReal();
    }
    catch ( Exception ex )
    {
      ex.printStackTrace();
    }
  }
  
  private final Path searchPath;
  private final Path intermediatePath;
  private final Path studentPath;
  private final boolean extract;
  private final boolean report;
  private final boolean digest;
  private boolean forcereport = false;

  private Transformer transformCombineModule;
  private Transformer transformListStudents;
  private Transformer transformRedactForStudent;
  private Transformer transformDigestArchive;
  private Transformer transformSummariseDigest;
  private Transformer transformStudentReport;
  private XPath xPath;
        
  ProcessProgress progress;
  private final BatchProcessorListener listener;
  
  public BatchProcessor( 
          BatchProcessorListener listener, 
          Path searchPath, 
          Path intermediatePath,
          Path studentPath,
          boolean extract,
          boolean report,
          boolean digest )
  {
    this.listener = listener;
    this.searchPath = searchPath;
    this.intermediatePath = intermediatePath;
    this.studentPath = studentPath;
    this.extract = extract;
    this.report = report;
    this.digest = digest;
  }

  public boolean isForceReport()
  {
    return forcereport;
  }

  public void setForceReport( boolean forcereport )
  {
    this.forcereport = forcereport;
  }

  private void init() throws IOException, TransformerConfigurationException
  {
    xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext( new NamespaceContextImpl() );

    TransformerFactory tFactory = TransformerFactory.newInstance();
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/combinemodule.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformCombineModule = templates.newTransformer();
    }
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/liststudents.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformListStudents = templates.newTransformer();
    }
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/redactforstudent.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformRedactForStudent = templates.newTransformer();
    }
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/studentreport.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformStudentReport = templates.newTransformer();
    }    
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/digestarchive.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformDigestArchive = templates.newTransformer();
    }    
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/summarisedigest.xsl" ) )
    {
      Templates templates = tFactory.newTemplates( new StreamSource( in ) );
      transformSummariseDigest = templates.newTransformer();
    }    
  }
  
  private Path getStudentPath( String courseid, ModuleUserInfo mui )
  {
    String part1 = mui.getSid().substring( 0, 2 ) + "______";
    String part2 = mui.getSid().substring( 0, 4 ) + "____";
    String part3 = mui.getSid().substring( 0, 6 ) + "__";
    return studentPath
            .resolve( part1 )
            .resolve( part2 )
            .resolve( part3 )
            .resolve( mui.getSid() )
            .resolve( "bblarchives" )
            .resolve( courseid );
  }

  private Path getStudentDataPath( String courseid, ModuleUserInfo mui )
  {
    return getStudentPath( courseid, mui ).resolve( "data.xml" );
  }

  
  @Override
  public void run()
  {
    Throwable caught = null;
    if ( listener != null )
      listener.batchProcessorStarted( this );
    try
    {
      processForReal();
    }
    catch ( Throwable th )
    {
      caught = th;
    }
    finally
    {
      if ( listener != null )
        listener.batchProcessorCompleted( this, caught );
    }
  }  
  
  private void processForReal() throws IOException, TransformerConfigurationException
  {
    init();
    
    if ( digest )
    {
      ArchiveDigestProcessor adp = new ArchiveDigestProcessor();
      progress = new ProcessProgress();
      ArchiveSearcher searcher = new ArchiveSearcher();
      Stream<Path> streamSearch = Files.find( searchPath, 2, searcher );
      streamSearch.forEach( adp );
      adp.close();
      if ( progress.firstException != null )
      {
        System.out.println( "Exception while processing stream of archives..." );
        progress.firstException.printStackTrace();
      }
    }
    
    // Get the data out of the archives...
    if ( extract )
    {
      progress = new ProcessProgress();
      ArchiveSearcher searcher = new ArchiveSearcher();
      Stream<Path> streamSearch = Files.find( searchPath, 2, searcher );
      streamSearch.forEach( new ArchiveCombinerProcessor() );
      if ( progress.firstException != null )
      {
        System.out.println( "Exception while processing stream of archives..." );
        progress.firstException.printStackTrace();
      }
    }
    
    // For each student rebuild the module/community reports
    if ( report )
    {
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
  }
  
  Path getRootOfArchive( Path t ) throws IOException
  {
    if ( Files.isDirectory( t ) )
      return t;
    FileSystem zipfs = FileSystems.newFileSystem( t, this.getClass().getClassLoader() );
    return zipfs.getPath( "/" );
  }  
  
  public class ProcessProgress
  {
    public int completedCount=0;
    public Exception firstException = null;
  }
  
  public class ArchiveCombinerProcessor implements Consumer<Path>
  {
    @Override
    public void accept( Path t )
    {
      if ( progress.firstException != null )
        return;
      
      System.out.println( "Processing " + t.toUri() );
      if ( listener != null )
        listener.batchProcessorProgress( BatchProcessor.this, t.toUri().toString(), 0, 0);
      
      // Set up coordinating data objects...
      Path root = null;
      DataLookupHelper datalookuphelper = null;
      ModuleUserInfoMapper mapper = null;
      FileProcessor fp = null;
      try
      {
        root = getRootOfArchive( t );          
        mapper = new ModuleUserInfoMapper();
        datalookuphelper = new DataLookupHelper( new DataLookups() );
        fp = new FileProcessor( root );
        process( root, mapper, datalookuphelper, fp );
        progress.completedCount++;
      }
      catch ( XPathExpressionException | TransformerException | ParserConfigurationException | IOException ex )
      {
        Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      finally
      {
        if ( root != null )
        {
          try {root.getFileSystem().close();}
          catch ( IOException ex ) {Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );}
        }
        if ( datalookuphelper != null ) datalookuphelper.release();
        if ( mapper != null ) mapper.release();
        if ( fp != null ) fp.release();
      }
    }
    
    private void process( Path root, ModuleUserInfoMapper mapper, DataLookupHelper datalookuphelper, FileProcessor fp )
            throws ParserConfigurationException, IOException, TransformerException, XPathExpressionException
    {      
      FileCataloguer fc = new FileCataloguer( root );
      Document catalogue = fc.getCatalogue();
      
      Resolver resolver = new Resolver( root, catalogue );
      StringBufferResult result = createStringBufferResult();
      HashMap<String,ModuleUserInfo> usermap;
      
      try ( InputStream in = Files.newInputStream( root.resolve( "imsmanifest.xml" ) ) )
      {
        StreamSource source = new StreamSource( in );
        transformListStudents.reset();
        transformListStudents.setParameter( "mapper-id", mapper.getId() );
        transformListStudents.setParameter( "data-lookups-id", datalookuphelper.getId() );
        transformListStudents.setURIResolver( resolver );
        transformListStudents.transform( source, result );
      }

      //System.out.println( result.toString() );
      usermap = mapper.getMap();
      if ( usermap.isEmpty() )
        return;

//      datalookups.dump();
//      if ( true ) return;

      DOMResult domresult = new DOMResult();
      try ( InputStream in = Files.newInputStream( root.resolve( "imsmanifest.xml" ) ) )
      {
        StreamSource source = new StreamSource( in );
        transformCombineModule.reset();
        transformCombineModule.setURIResolver( resolver );
        transformCombineModule.transform( source, domresult );
      }

      System.out.println( domresult.toString() );
      String courseid = xPath.evaluateExpression(
                "/bbcarv:resources/bbcarv:resource[@type='course/x-bb-coursesetting']/bbcarv:data/COURSE/COURSEID/@value",
                domresult.getNode(),
                String.class );
      
      if ( courseid == null || courseid.length() == 0 )
        throw new IOException( "Course ID not found in archive." );        
      System.out.println( "Course ID = " + courseid );

      for ( ModuleUserInfo mui : usermap.values() )
      {
        System.out.println( "Working on " + mui.toString() );
        Path studentdir = getStudentPath( courseid, mui );
        Path studentdata = getStudentDataPath( courseid, mui );
        fp.setDestinationBase( studentdir );
        Files.createDirectories( studentdata.getParent() );
        StreamResult sresult = new StreamResult( studentdata.toFile() );
        DOMSource source = new DOMSource( domresult.getNode() );
        transformRedactForStudent.reset();
        transformRedactForStudent.setParameter( "data-lookups-id", datalookuphelper.getId() );
        transformRedactForStudent.setParameter( "file-processor-id", fp.getId() );
        transformRedactForStudent.setParameter( "uid", mui.getUid() );
        transformRedactForStudent.setParameter( "sid", mui.getSid() );
        transformRedactForStudent.setParameter( "mid", mui.getMid() );
        transformRedactForStudent.setURIResolver( resolver );
        transformRedactForStudent.transform( source, sresult );
      }      
    }
    
  }

  public class ArchiveDigestProcessor implements Consumer<Path>
  {
    Path f;
    Writer writer;
    Path s;
    
    public ArchiveDigestProcessor()
    {
      f = studentPath.resolve( "digest_" + System.currentTimeMillis() + ".xml" );
      s = studentPath.resolve( "summary_" + System.currentTimeMillis() + ".xml" );
      try
      {
        writer = Files.newBufferedWriter( f );
        writer.write( "<?xml version=\"1.0\"?>\n" );
        writer.write( "<digest>\n" );
      }
      catch ( IOException ex )
      {
        Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );
        writer = null;
      }
    }
    
    public void close()
    {
      try
      {
        writer.write( "</digest>\n" );
        writer.close();
      }
      catch ( IOException ex )
      {
        Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );
        throw new RuntimeException( ex );
      }
      
      try ( InputStream in = Files.newInputStream( f ); Writer swriter = Files.newBufferedWriter( s ) )
      {
        StreamSource source = new StreamSource( in );
        StreamResult result = new StreamResult( swriter );
        transformSummariseDigest.reset();
        transformSummariseDigest.transform( source, result );
        System.out.println( result.toString() );
      }
      catch ( IOException | TransformerException ex )
      {
        Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );
        throw new RuntimeException( ex );
      }
    }
    
    @Override
    public void accept( Path t )
    {
      if ( progress.firstException != null )
        return;
      
      System.out.println( "Processing " + t.toUri() );
      if ( listener != null )
        listener.batchProcessorProgress( BatchProcessor.this, t.toUri().toString(), 0, 0);
      
      Path root = null;
      try
      {
        root = getRootOfArchive( t );  
      }
      catch ( IOException ex )
      {
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      
      Resolver resolver = new Resolver( root, null );
     
      try ( InputStream in = Files.newInputStream( root.resolve( "imsmanifest.xml" ) ) )
      {
        StreamSource source = new StreamSource( in );
        StringBufferResult result = createStringBufferResult();
        transformDigestArchive.reset();
        transformDigestArchive.setURIResolver( resolver );
        transformDigestArchive.setParameter( "filename", t.getFileName().toString() );
        transformDigestArchive.transform( source, result );
        //System.out.println( result.toString() );
        writer.write( result.toString() );
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
      try ( InputStream in = Files.newInputStream( t );
            OutputStream out = Files.newOutputStream( report ) )
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
  
  public class ArchiveSearcher implements BiPredicate<Path,BasicFileAttributes>
  {
    @Override
    public boolean test( Path t, BasicFileAttributes u )
    {
      // Will accept either zip file or directory named with '.zip'
      if ( !t.getFileName().toString().endsWith( ".zip" ) )
        return false;
      return Files.exists( t );
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
      if ( forcereport ) return true;
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
      try
      {
        return new StreamSource( Files.newInputStream( archiveBase.resolve( href ) ) );
      }
      catch ( IOException ex )
      {
      }
      return null;
    }
    
  }
  
  public class NamespaceContextImpl implements NamespaceContext
  {

    @Override
    public String getNamespaceURI( String prefix )
    {
      switch ( prefix )
      {
        case "bbcarv": return "http://www.leedsbeckett.ac.uk/bubblecarver";
        default: return "";
      }
    }

    @Override
    public String getPrefix( String namespaceURI )
    {
      throw new UnsupportedOperationException( "Not supported yet." ); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Iterator<String> getPrefixes( String namespaceURI )
    {
      throw new UnsupportedOperationException( "Not supported yet." ); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
  }
}
