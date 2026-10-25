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

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.xml.stream.*;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.data.GroupMap;
import uk.ac.leedsbeckett.bubblecarver.data.WantedFiles;
import uk.ac.leedsbeckett.bubblecarver.stax.StaxFileCataloguer;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;
import uk.ac.leedsbeckett.bubblecarver.util.Utils;
import uk.ac.leedsbeckett.bubblecarver.zip.ZipProcessor;

/**
 * Combines XML files into one big XML file with attributes added that will
 * aid the process of redacting the XML for each student's view of it.
 * 
 * Pass 1a - find out which files in the archive are XML files that need
 * to be included in the big XML file.
 * Pass 1b - combine the XML files into one big one AND index on the fly.
 * 
 * Pass 2 - add attributes to the big XML to indicate which data belongs
 * to which student or which group of students.
 * 
 * Some indexing data will be passed on to splitter to aid redaction and
 * selection of binary files per student.
 * 
 * @author maber01
 */
public class BubbleCombiner
{
  // Properties hard codes date/time output in US English but local time zone
  // GB formatter must be avoided because of silly four letter abbv for Sept
  public final static DateTimeFormatter packageDTFormatter =  DateTimeFormatter.ofPattern( "yyyy_MM_dd_HH.mm.ss" );
  public final static ArrayList<String> standardFiles = new ArrayList<>( Arrays.asList( new String[]
        {
          "imsmanifest.xml",
          ".bb-package-info",
          "csfiles/privatedoc/gradebook/*"
        }
  ));
  
  
  BubbleCombinerContext context;
  GeneralMap generalMap;
  GroupMap groupMap;
  WantedFiles wantedFiles = null;
  List<String> interestingStudentIds;
  final Properties packageInfo = new Properties();
  
  final Path archiveBasePath;
  final Path outputBasePath;
  final ZipProcessor zipper;
  
  Path courseBasePath;
  
  String courseId;
  String packageTime;
  String archiveDirectoryName;
  
  public BubbleCombiner( Path archiveBasePath, Path outputBasePath, ZipProcessor zipper )
  {
    this.archiveBasePath = archiveBasePath;
    this.outputBasePath = outputBasePath;
    this.zipper = zipper;
  }
  
  public void process() throws IOException, XMLStreamException
  {
    Path packageInfoPath = archiveBasePath.resolve( ".bb-package-info" );
    String dateline = null;
    
    try ( InputStream infoIn = Files.newInputStream( packageInfoPath );
          InputStreamReader  reader = new InputStreamReader( infoIn, ISO_8859_1 );
          BufferedReader linereader = new BufferedReader( reader ) )
    {
      // discard first comment line
      linereader.readLine();
      // second comment line should contain date/time stamp;
      dateline = linereader.readLine();
    }
    LocalDateTime stamp = Utils.parsePropertiesDateTime( dateline );
    
    packageTime = stamp.format( packageDTFormatter );
    try ( InputStream infoIn = Files.newInputStream( packageInfoPath ) )
    {
      packageInfo.load( infoIn );
    }
    
    courseId = packageInfo.getProperty( "cx.config.course.id" );
    //get package time from comments in properties file not time stamp
    //because the time zone of that isn't clear.
    //packageTime = Files.getLastModifiedTime( packageInfoPath ).toInstant().toString().replace( ':', '-' );
    archiveDirectoryName = courseId + "__" + packageTime;

    courseBasePath = outputBasePath.resolve( "courses" ).resolve( archiveDirectoryName );
    Files.createDirectories( courseBasePath );
    context = new BubbleCombinerContext( archiveBasePath, courseBasePath );
    
    try ( InputStream in = Files.newInputStream( context.inputPath );
          FileWriter fileWriter = new FileWriter( context.pass1aPath.toFile() );
            )
    {
      passOne( in, fileWriter, true );
    }      
    
    generalMap.dump( courseBasePath.resolve( "generalmap1.txt" ) );    

    ArrayList<String> list = new ArrayList<>();
    for ( Path f : wantedFiles.getForCombiner() )
      list.add( archiveBasePath.relativize( f ).toString() );
    zipper.extract( list );
      
    try ( InputStream in = Files.newInputStream( context.inputPath );
          FileWriter fileWriter = new FileWriter( context.pass1bPath.toFile() );
            )
    {
      passOne( in, fileWriter, false );      
    }

    generalMap.dump( courseBasePath.resolve( "generalmap2.txt" ) );    
    
    try ( InputStream in = Files.newInputStream( context.pass1bPath );
          FileWriter fileWriter = new FileWriter( context.outputPath.toFile() );
            )
    {
      passTwo( in, fileWriter );
    }

    list = new ArrayList<>();
    for ( Path f : wantedFiles.getForSplitter() )
      list.add( archiveBasePath.relativize( f ).toString() );
    zipper.extract( list );
  }
  
  public void passOne( InputStream in, FileWriter fileWriter, boolean dummyRun ) throws XMLStreamException, IOException
  {
    XMLEventReader reader = context.getReaderFactory().createXMLEventReader( in );
    XMLStreamWriter wrappedwriter = context.getWriteFactory().createXMLStreamWriter( fileWriter );
    IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter( wrappedwriter );
    StaxFilterConfiguration config = new StaxFilterConfiguration();
    config.outputFragment = true;
    FullFilterContext<BubbleCombinerContext,ReaderNodeContext,WriterNodeContext> ffc 
            = new FullFilterContext<>( context );
    
    ManifestFilter filter = new ManifestFilter( reader, writer, ffc, config, dummyRun );
    
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeCharacters( "\n" );
    writer.writeStartElement( "bbcarv", "archive", Names.BCARVNAMESPACE );
    writer.writeNamespace( "bbcarv", Names.BCARVNAMESPACE );
    writer.writeNamespace( "cat", Names.CATALOGUENAMESPACE );
    writer.writeAttribute( 
            Names.BCARVCOURSEIDATTRIBUTE.getNamespaceURI(), 
            Names.BCARVCOURSEIDATTRIBUTE.getLocalPart(), 
            courseId );
    writer.writeAttribute( 
            Names.BCARVARCHIVETIMEATTRIBUTE.getNamespaceURI(), 
            Names.BCARVARCHIVETIMEATTRIBUTE.getLocalPart(), 
            packageTime );
    writer.writeCharacters( "\n" );
    filter.process();
    
    // Put a list of files from the archive at the end of the output
    if ( !dummyRun )
    {
      StaxFileCataloguer fcat = new StaxFileCataloguer( archiveBasePath );
      fcat.writeCatalogue( writer, true );
    }
    
    writer.writeEndElement();
    writer.writeCharacters( "\n" );
    writer.writeEndDocument();
    writer.writeCharacters( "\n" );
    generalMap = filter.getContext().getFilterContext().getGeneralMap();
    groupMap = filter.getContext().getFilterContext().getGroupMap();
    if ( dummyRun )
      wantedFiles = filter.getWantedFiles();
  }
  
  public void passTwo( InputStream in, FileWriter fileWriter ) throws XMLStreamException, IOException
  {
    XMLEventReader reader = context.getReaderFactory().createXMLEventReader( in );
    XMLStreamWriter writer = context.getWriteFactory().createXMLStreamWriter( fileWriter );
    //IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter( wrappedwriter );
    StaxFilterConfiguration config = new StaxFilterConfiguration();
    config.outputFragment = true;
    FullFilterContext<BubbleCombinerContext,ReaderNodeContext,WriterNodeContext> ffc 
            = new FullFilterContext<>( context );
    
    SecondPassFilter filter = new SecondPassFilter( reader, writer, ffc, config );
    
    writer.writeStartDocument("UTF-8", "1.0");
    filter.process();
    writer.writeEndDocument();
    generalMap = filter.getContext().getFilterContext().getGeneralMap();
    interestingStudentIds = filter.getContext().getFilterContext().getInterestingStudentIds();
  }

  public Path getCourseBasePath()
  {
    return courseBasePath;
  }
  
  public String getCourseId() {
    return courseId;
  }

  public String getPackageTime() {
    return packageTime;
  }

  public String getArchiveDirectoryName() {
    return archiveDirectoryName;
  }

  public GeneralMap getGeneralMap()
  {
    return generalMap;
  }

  public GroupMap getGroupMap()
  {
    return groupMap;
  }
  
  public List<String> getInterestingStudentIds()
  {
    return interestingStudentIds;
  }
  
  
}
