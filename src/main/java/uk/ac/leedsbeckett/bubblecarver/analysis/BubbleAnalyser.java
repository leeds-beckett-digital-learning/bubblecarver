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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.*;
import uk.ac.leedsbeckett.bubblecarver.combiner.Names;
import uk.ac.leedsbeckett.bubblecarver.stax.StaxFileCataloguer;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;
import uk.ac.leedsbeckett.bubblecarver.zip.ZipProcessor;

/**
 *
 * @author maber01
 */
public class BubbleAnalyser
{
  public static ArrayList<String> standardFiles = new ArrayList<>( Arrays.asList( new String[]
        {
          "imsmanifest.xml",
          ".bb-package-info",
        }
  ));
  
  
  BubbleAnalyserContext context;
  final Properties packageInfo = new Properties();
  
  final Path archiveBasePath;
  final XMLStreamWriter writer;
  List<Path> wantedFiles = null;
  final ZipProcessor zipper;
  
  String courseId;
  String packageTime;
  
  public BubbleAnalyser( Path archiveBasePath, XMLStreamWriter writer, ZipProcessor zipper )
  {
    this.archiveBasePath = archiveBasePath;
    this.writer = writer;
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

    context = new BubbleAnalyserContext( archiveBasePath, packageInfo );

    try ( InputStream in = Files.newInputStream( context.inputPath ); )
    {
      passOne( in, writer, true );
    }      

    ArrayList<String> list = new ArrayList<>();
    for ( Path f : wantedFiles )
    {
      String s = archiveBasePath.relativize( f ).toString();
      list.add( s.replaceAll( "wildcard", "*" ) );
    }
    zipper.extract( list );

    try ( InputStream in = Files.newInputStream( context.inputPath ); )
    {
      passOne( in, writer, false );      
    }      
  }
  
  public void passOne( InputStream in, XMLStreamWriter writer, boolean dummyRun ) throws XMLStreamException, IOException
  {
    XMLEventReader reader = context.getReaderFactory().createXMLEventReader( in );
    StaxFilterConfiguration config = new StaxFilterConfiguration();
    config.outputFragment = true;
    FullFilterContext<BubbleAnalyserContext,ReaderNodeContext,WriterNodeContext> ffc 
            = new FullFilterContext<>( context );
    
    ManifestFilter filter = new ManifestFilter( reader, writer, ffc, config, dummyRun );
    
    if ( !dummyRun )
    {
      writer.writeStartElement( "bbcarv", "archive", Names.BCARVNAMESPACE );
      writer.writeNamespace( "bbcarv", Names.BCARVNAMESPACE );
      writer.writeAttribute( 
              Names.BCARVCOURSEIDATTRIBUTE.getNamespaceURI(), 
              Names.BCARVCOURSEIDATTRIBUTE.getLocalPart(), 
              courseId );
      writer.writeAttribute( 
              Names.BCARVARCHIVETIMEATTRIBUTE.getNamespaceURI(), 
              Names.BCARVARCHIVETIMEATTRIBUTE.getLocalPart(), 
              packageTime );
      writer.writeCharacters( "\n" );
    }
    
    filter.process();
    if ( dummyRun )
      wantedFiles = filter.getWantedFiles();
    
    if ( !dummyRun )
    {
      wantedFiles = filter.getWantedFiles();
      writer.writeEndElement();
      writer.writeCharacters( "\n" );
    }
  }
  
  public String getCourseId() {
    return courseId;
  }

  public String getPackageTime() {
    return packageTime;
  }

}
