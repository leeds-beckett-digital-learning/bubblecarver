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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.*;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.stax.StaxFileCataloguer;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;
import uk.ac.leedsbeckett.bubblecarver.zip.ZipProcessor;

/**
 *
 * @author maber01
 */
public class BubbleCombiner
{
  public static ArrayList<String> standardFiles = new ArrayList<>( Arrays.asList( new String[]
        {
          "imsmanifest.xml",
          ".bb-package-info",
          "csfiles/privatedoc/gradebook/*"
        }
  ));
  
  
  BubbleCombinerContext context;
  GeneralMap generalMap;
  List<Path> wantedFiles = null;
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
    try ( InputStream infoIn = Files.newInputStream( packageInfoPath ) )
    {
      packageInfo.load( infoIn );
    }
    
    courseId = packageInfo.getProperty( "cx.config.course.id" );
    packageTime = Files.getLastModifiedTime( packageInfoPath ).toInstant().toString().replace( ':', '-' );
    packageInfo.setProperty( "bbcarv.time", packageTime );
    archiveDirectoryName = courseId + "__" + packageTime;

    courseBasePath = outputBasePath.resolve( "courses" ).resolve( archiveDirectoryName );
    Files.createDirectories( courseBasePath );
    context = new BubbleCombinerContext( archiveBasePath, courseBasePath, packageInfo );
    
    try ( InputStream in = Files.newInputStream( context.inputPath );
          FileWriter fileWriter = new FileWriter( context.pass1aPath.toFile() );
            )
    {
      passOne( in, fileWriter, true );
    }      

    ArrayList<String> list = new ArrayList<>();
    for ( Path f : wantedFiles )
    {
      String s = archiveBasePath.relativize( f ).toString();
      list.add( s.replaceAll( "wildcard", "*" ) );
    }
    zipper.extract( list );
      
    try ( InputStream in = Files.newInputStream( context.inputPath );
          FileWriter fileWriter = new FileWriter( context.pass1bPath.toFile() );
            )
    {
      passOne( in, fileWriter, false );      
    }
    
    try ( InputStream in = Files.newInputStream( context.pass1bPath );
          FileWriter fileWriter = new FileWriter( context.outputPath.toFile() );
            )
    {
      passTwo( in, fileWriter );
    }
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

  
  public List<String> getInterestingStudentIds()
  {
    return interestingStudentIds;
  }
  
  
}
