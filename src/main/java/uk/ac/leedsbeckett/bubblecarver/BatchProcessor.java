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

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import java.io.FileWriter;
import uk.ac.leedsbeckett.bubblecarver.data.ModuleUserInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.xml.sax.SAXException;
import uk.ac.leedsbeckett.bubblecarver.analysis.BubbleAnalyser;
import uk.ac.leedsbeckett.bubblecarver.combiner.BubbleCombiner;
import uk.ac.leedsbeckett.bubblecarver.combiner.Names;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.data.GroupMap;
import uk.ac.leedsbeckett.bubblecarver.data.ProcessorSession;
import uk.ac.leedsbeckett.bubblecarver.exceptions.UpToDateException;
import uk.ac.leedsbeckett.bubblecarver.reporter.BubbleReporter;
import uk.ac.leedsbeckett.bubblecarver.splitter.BubbleSplitter;
import uk.ac.leedsbeckett.bubblecarver.util.PathResolver;
import uk.ac.leedsbeckett.bubblecarver.util.Utils;
import uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper;
import uk.ac.leedsbeckett.bubblecarver.xsl.DataLookups;
import uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor;
import uk.ac.leedsbeckett.bubblecarver.xsl.GeneralMappingHelper;
import uk.ac.leedsbeckett.bubblecarver.xsl.ModuleUserInfoMapper;
import uk.ac.leedsbeckett.bubblecarver.zip.ZipProcessor;

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
    Path out    = Path.of( "c:\\temp\\students" );
    try
    {
      // To do - need to add configuration loading.
      BubbleCarverConfig config = new BubbleCarverConfig();
      try {
        config.load();
      } catch ( IOException ex ) {
        System.err.println( "Unable to load/create configuration file.");
        System.exit( 1 );
      }
      ZipProcessor.setZipExecPath( config.get7ZaExecutableFileName() );
      BatchProcessor bp = new BatchProcessor( config, null, in, out, null, true, true, false );
      bp.setForceReport( true );
      bp.processForReal();
    }
    catch ( Exception ex )
    {
      ex.printStackTrace();
    }
  }
  
  private final Path searchPath;
  private final Path studentIDPath;
  private final PathResolver pathResolver;
  
  private final boolean extract;
  private final boolean report;
  private final boolean digest;
  private boolean forcereport = false;

  Path telemetryPath;
  
  private Transformer transformDigestArchive;
  private Transformer transformSummariseDigest;
  private Transformer transformStudentReport;
  private XPath xPath;
        
  ProcessProgress progress;
  private final BatchProcessorListener listener;
  private final BubbleCarverConfig config;
  
  Logger telemetryLog;
  Handler telemetrySessionHandler;
    
  public BatchProcessor( 
          BubbleCarverConfig config,
          BatchProcessorListener listener, 
          Path searchPath, 
          Path outputPath,
          Path studentIDPath,
          boolean extract,
          boolean report,
          boolean digest )
  {
    this.config = config;
    this.listener = listener;
    this.searchPath = searchPath;
    this.studentIDPath = studentIDPath;
    this.pathResolver = new PathResolver( outputPath );
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
    telemetryPath = pathResolver.getWorkspacePath( "telemetry" );
    Files.createDirectories( telemetryPath );
    telemetryLog = Logger.getLogger( "BatchProcessor" + Utils.getNowString() );
    telemetryLog.setUseParentHandlers( false );
    // Will reset log and apply handler for each archive file.
    telemetrySessionHandler = getSessionHandler();
            
    xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext( new NamespaceContextImpl() );

    TransformerFactory tFactory = TransformerFactory.newInstance();
    try ( InputStream in = BatchProcessor.class.getResourceAsStream( "/studentreportnew.xsl" ) )
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
  
  @Override
  public void run()
  {
    Throwable caught = null;
    if ( listener != null )
      listener.batchProcessorStarted( this );
    try
    {
      init();
      caught = processForReal();
    }
    catch ( Throwable th )
    {
      caught = th;
      th.printStackTrace();
    }
    finally
    {
      if ( telemetrySessionHandler != null )
        telemetrySessionHandler.close();
      if ( listener != null )
        listener.batchProcessorCompleted( this, caught );
    }
  }  
  
  private Throwable processForReal() throws IOException, TransformerConfigurationException, XMLStreamException
  {    
    if ( digest )
    {
      progress = new ProcessProgress();
      Path reportPath = pathResolver.resolve( "report_" + Utils.getNowString() + ".xml" );
      Files.createDirectories( reportPath.getParent() );
      try ( FileWriter fileWriter = new FileWriter( reportPath.toFile() ) )
      {
        XMLOutputFactory writeFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter wrappedwriter = writeFactory.createXMLStreamWriter( fileWriter );
        IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter( wrappedwriter );
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement( "bbcarv", "archives", Names.BCARVNAMESPACE );
        writer.writeNamespace( "bbcarv", Names.BCARVNAMESPACE );
        ArchiveSearcher searcher = new ArchiveSearcher();
        Stream<Path> streamSearch = Files.find( searchPath, Integer.MAX_VALUE, searcher );
        streamSearch.forEach( new ArchiveAnalysisProcessor( writer ) );
        if ( progress.firstException != null )
        {
          System.out.println( "Exception while processing stream of archives..." );
          progress.firstException.printStackTrace();
          return progress.firstException;
        }
        writer.writeEndElement();
        writer.writeEndDocument();
      }
    }
    
    // Get the data out of the archives...
    if ( extract )
    {
      progress = new ProcessProgress();
      ArchiveSearcher searcher = new ArchiveSearcher();
      Stream<Path> streamSearch = Files.find( searchPath, Integer.MAX_VALUE, searcher );
      streamSearch.forEach( new ArchiveCombinerProcessor() );
      if ( progress.firstException != null )
      {
        System.out.println( "Exception while processing stream of archives..." );
        progress.firstException.printStackTrace();
        return progress.firstException;
      }
    }
    
    // For each student rebuild the module/community reports
    if ( report )
    {
      progress = new ProcessProgress();
      String style  = FileUtils.readFileToString( pathResolver.getCssPath( "blackboard.css" ).toFile(), StandardCharsets.UTF_8 );
      BubbleReporter bubbleResporter = new BubbleReporter( transformStudentReport, style );
      StudentArchiveDataSearcher sdsearcher = new StudentArchiveDataSearcher( studentIDPath );
      Stream<Path> studentArchiveDataSearch = Files.find( pathResolver.getStudentsPath(), Integer.MAX_VALUE, sdsearcher );
      studentArchiveDataSearch.forEach( new StudentArchiveDataProcessor( bubbleResporter ) );
      if ( progress.firstException != null )
      {
        System.out.println( "Exception while processing stream of archives..." );
        progress.firstException.printStackTrace();
        return progress.firstException;
      }    
    }
    
    return null;
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
  
  /**
   * Takes archive zip files one at a time and processes them.
   * Two phases - combining many XML files from zip into one XML
   * file and then splitting that into redacted copies, one for each
   * student. As part of the splitting process relevant binary files
   * are copied into the student directories.
   */
  public class ArchiveCombinerProcessor implements Consumer<Path>
  {
    Telemetry telemetry;
    
    @Override
    public void accept( Path t )
    {
      Path extractPath = null;
      
      if ( progress.firstException != null )
        return;
      
      System.out.println( "Processing " + t.toUri() );
      if ( listener != null )
        listener.batchProcessorProgress( BatchProcessor.this, t.toUri().toString(), 0, 0);
      
      telemetry = new Telemetry( t, telemetrySessionHandler );
      telemetry.startArchive();
      
      // Set up coordinating data objects...
      ProcessorSession session = new ProcessorSession();
      try
      {
        extractPath = pathResolver.getZipPath( Long.toHexString( System.currentTimeMillis() ) );
        Files.createDirectories( extractPath );
        
        session.zipper = new ZipProcessor( t, extractPath );
        session.zipper.extract( BubbleCombiner.standardFiles );
    
        session.processingdir    = pathResolver.getBasePath();
        Files.createDirectories( session.processingdir );
        session.root             = extractPath;          
        session.mapper           = new ModuleUserInfoMapper();
        session.datalookuphelper = new DataLookupHelper( new DataLookups() );
        session.fp               = new FileProcessor( session.root );
        session.generalmapper    = new GeneralMappingHelper();
        process( session );
      }
      catch ( UpToDateException utdex )
      {
        System.out.println( "Course archive has already been processed. Skipping." );
      }
      catch ( Exception ex )
      {
        Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      finally
      {
        progress.completedCount++;
        telemetry.completeArchive();
        session.release();
        if ( extractPath != null && Files.exists( extractPath ) )
        {
          try
          {
            FileUtils.deleteDirectory( extractPath.toFile() );
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
            // carry on anyway - get on with next archive
          }
        }

      }
    }
    
    private void process( ProcessorSession session )
            throws ParserConfigurationException, IOException, TransformerException, XPathExpressionException, XMLStreamException, UpToDateException
    {      
      BubbleCombiner bc = new BubbleCombiner( session.root, pathResolver, session.zipper );
      bc.process();

      Path courseBasePath = bc.getCourseBasePath();
      GeneralMap genmap = bc.getGeneralMap();
      GroupMap groupmap = bc.getGroupMap();
      groupmap.rebuild( genmap );
      bc.getResourceUsageSet().dump( courseBasePath.resolve( "resourceusagea.txt" ) );
      bc.getResourceUsageSet().rebuild( groupmap );
      bc.getResourceUsageSet().dump( courseBasePath.resolve( "resourceusageb.txt" ) );

      BubbleSplitter bs = new BubbleSplitter(
          courseBasePath.getFileName().toString(),
          courseBasePath.resolve( "pass_two_archive.xml" ),
          session.root,
          pathResolver,
          bc.getInterestingStudentIds(),
          bc.getResourceUsageSet(),
          groupmap
        );
      bs.process();
      
      Path completed = pathResolver.getCompletedFilePath( courseBasePath );
      if ( !Files.exists( completed ) )
        Files.createFile( completed );
    }
  }

  public class ArchiveAnalysisProcessor implements Consumer<Path>
  {
    final XMLStreamWriter writer;

    public ArchiveAnalysisProcessor( XMLStreamWriter writer )
    {
      this.writer = writer;
    }
    
    @Override
    public void accept( Path t )
    {
      Path extractPath = null;
      if ( progress.firstException != null )
        return;
      System.out.println( "Processing " + t.toUri() );
      if ( listener != null )
        listener.batchProcessorProgress( BatchProcessor.this, t.toUri().toString(), 0, 0);
      
      ProcessorSession session = new ProcessorSession();
      try
      {
        extractPath = pathResolver.getZipPath( Long.toHexString( System.currentTimeMillis() ) );
        Files.createDirectories( extractPath );
        session.zipper = new ZipProcessor( t, extractPath );
        session.zipper.extract( BubbleCombiner.standardFiles );
    
        session.processingdir    = pathResolver.getBasePath();
        Files.createDirectories( session.processingdir );
        session.root             = extractPath;          
        process( session );
        progress.completedCount++;
      }
      catch ( Exception ex )
      {
        Logger.getLogger( BatchProcessor.class.getName() ).log( Level.SEVERE, null, ex );
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      finally
      {
        session.release();
        if ( extractPath != null && Files.exists( extractPath ) )
        {
          try
          {
            FileUtils.deleteDirectory( extractPath.toFile() );
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
            // carry on anyway - get on with next archive
          }
        }
      }
    }
    
    private void process( ProcessorSession session )
            throws ParserConfigurationException, IOException, TransformerException, XPathExpressionException, XMLStreamException
    {      
      BubbleAnalyser bc = new BubbleAnalyser( session.root, writer, session.zipper );
      bc.process();
    }
  }

  
  public class StudentArchiveDataProcessor implements Consumer<Path>
  {    
    final BubbleReporter bubbleReporter;

    public StudentArchiveDataProcessor(BubbleReporter bubbleReporter)
    {
      this.bubbleReporter = bubbleReporter;
    }
    
    
    
    @Override
    public void accept( Path t )
    {
      if ( progress.firstException != null )
        return;
      System.out.println( "Processing " + t.toUri() );
      try 
      {
        bubbleReporter.processStudent( t );
      }
      catch ( IOException | TransformerException | ParserConfigurationException | SAXException ex )
      {
        progress.firstException = ex;
        throw new RuntimeException( ex );
      }
      progress.completedCount++;
    }
  }
  
  /**
   * No attempt in this searcher to skip archives that have already been
   * processed because that can't be done without extracting a file from
   * the ZIP.  Let the processor itself decide whether to skip an archive.
   * (Because we the target directory path depends on the date/time that
   * the course archive was created and that is in a property file packed
   * in the ZIP.)
   */
  public class ArchiveSearcher implements BiPredicate<Path,BasicFileAttributes>
  {
    @Override
    public boolean test( Path t, BasicFileAttributes u )
    {
      System.out.println( "ArchiveSearcher.test( " + t.toString() + ")" );
      // No longer accepting directories - ZIPs only.
      if ( Files.isDirectory( t ) )
        return false;
      
      if ( !t.getFileName().toString().endsWith( ".zip" ) )
        return false;
      System.out.println( "             Looks like archive." );
      return Files.exists( t );
    }
  }  
  
  public class StudentArchiveDataSearcher implements BiPredicate<Path,BasicFileAttributes>
  {
    final HashSet<String> sidSet;
    
    public StudentArchiveDataSearcher( Path studentIDPath ) throws IOException
    {
      if ( studentIDPath == null )
        sidSet = null;
      else
      {
        String studentInput = IOUtils.toString( studentIDPath.toUri(), StandardCharsets.UTF_8 );
        String[] parts = studentInput.split( "[^0-9]" );
        sidSet = new HashSet<>();
        for ( String sid : parts )
        {
          if ( sid.length() == 8 )
            sidSet.add( sid );
        }      
      }
    }
    
    @Override
    public boolean test( Path t, BasicFileAttributes u )
    {
      if ( !u.isRegularFile() )
        return false;
      if ( !t.getFileName().toString().equals( "redacted_archive_data.xml") )
        return false;
      
      // If student list was specified check this one.
      if ( sidSet != null )
      {
        // parent directory name of redacted_archive_data.xml
        String s = t.getName( t.getNameCount() - 4 ).toString();
        if ( !sidSet.contains( s ) )
          return false;
      }
      
      if ( forcereport ) return true;
      
      Path reppath = t.getParent().resolve( "index.html" );
      if ( !Files.exists( reppath ) ) return true;  // No output so process
      try
      {
        BasicFileAttributes repatt = Files.readAttributes( reppath, BasicFileAttributes.class );
        if ( repatt.lastModifiedTime().compareTo( u.lastModifiedTime() ) < 0 )
          return true;
      }
      catch ( IOException ex )
      {
        // Can' work out dates? Update anyway.
        return true;
      }
      
      // Report is up to date so don't refresh.
      return false;
    }

    public boolean testold( Path t, BasicFileAttributes u )
    {
      if ( !u.isRegularFile() )
        return false;
      if ( !t.getFileName().toString().equals( "data.xml") )
        return false;
      Path relpath = pathResolver.getBasePath().relativize( t );
      if ( relpath.getNameCount() != 9 ) return false;
      if ( !"bblarchives".equals( relpath.getName( 4 ).toString() ) )
        return false;
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
  
  public class TelemetryFormatter extends Formatter
  {
    final boolean withTimestamp;
    final boolean withArchivePath;
    StringBuilder sb = new StringBuilder();

    public TelemetryFormatter(boolean withTimestamp,boolean withArchivePath)
    {
      this.withTimestamp   = withTimestamp;
      this.withArchivePath = withArchivePath;
    }

    @Override
    public String format( LogRecord record )
    {
      Object[] params = record.getParameters();
      TelemetryData data = null;
      
      if ( params != null && params.length > 0 && params[0] instanceof TelemetryData )
        data = (TelemetryData)params[0];
      
      sb.setLength( 0 );
      if ( withTimestamp )
      {
        sb.append( Utils.getNowString() );
        sb.append( ",    " );
      }
      if ( withArchivePath )
      {
        if ( data != null )
          sb.append( data.archivePath.toString() );
        sb.append( ",    " );        
      }
      sb.append( record.getMessage() );
      if ( data != null )
      {
        if ( data.milliSecondDuration >= 0 )
        {
          sb.append( ",    " );
          sb.append( data.milliSecondDuration );
          sb.append( ",    " );
          sb.append( DurationFormatUtils.formatDurationHMS( data.milliSecondDuration ) );
        }
      }
      sb.append( "\n" );
      return sb.toString();
    }
  }

  Handler getSessionHandler()
  {
    try {
      TelemetryFormatter formatter = new TelemetryFormatter( true, true );
      String nowString = Utils.getNowString();
      Path telemetryFilePath = telemetryPath.resolve( "session_" + nowString + ".csv" );
      FileHandler sessionHandler = new FileHandler(
              telemetryFilePath.toString(),
              true );
      sessionHandler.setFormatter( formatter );
      return sessionHandler;
    } catch (IOException | SecurityException ex) {
      System.getLogger(BatchProcessor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
    }
    return null;
  }

  public class TelemetryData
  {
    final Path archivePath;
    final long milliSecondDuration;

    public TelemetryData(Path archivePath)
    {
      this.archivePath = archivePath;
      this.milliSecondDuration = -1;
    }

    public TelemetryData(Path archivePath, long milliSecondDuration)
    {
      this.archivePath = archivePath;
      this.milliSecondDuration = milliSecondDuration;
    }

    public Path getArchivePath()
    {
      return archivePath;
    }

    public long getMilliSecondDuration()
    {
      return milliSecondDuration;
    }
  }
  
  public class Telemetry
  {    
    TelemetryFormatter formatter = new TelemetryFormatter( false, false );

    FileHandler detailHandler;

    Path archivePath;
    long startArchive;
    
    public Telemetry( Path archivePath, Handler sessionHandler )
    {
      this.archivePath = archivePath;
      try
      {
        for ( Handler h : telemetryLog.getHandlers() )
          telemetryLog.removeHandler( h );
        String nowString = Utils.getNowString();
        String telemetryName = archivePath.getFileName().toString() + "_" + nowString + "_telemetry.txt";
        Path telemetryFilePath = telemetryPath.resolve( telemetryName );
        detailHandler = new FileHandler(
                telemetryFilePath.toString(),
                true );
        detailHandler.setFormatter( formatter );
        telemetryLog.addHandler( sessionHandler );
        telemetryLog.addHandler( detailHandler );
        
        telemetryLog.fine( "Telemetry log opened." );
      }
      catch (IOException | SecurityException ex)
      {
        System.getLogger(BatchProcessor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
      }
    }
    
    public void startArchive()
    {
      telemetryLog.fine( "Started processing archive." );
      startArchive = System.currentTimeMillis();
    }

    public void completeArchive()
    {
      if ( telemetryLog == null ) return;
      long duration = System.currentTimeMillis() - startArchive;
      String message = "Completed archive";
      TelemetryData data = new TelemetryData( archivePath, duration );
      telemetryLog.log( Level.INFO, message, new Object[] { data } );
      
      telemetryLog.removeHandler(detailHandler );
      detailHandler.close();
    }
  }
}
