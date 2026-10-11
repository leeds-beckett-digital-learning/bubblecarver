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

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.combiner.Names;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.FullFilterContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.ReaderNodeContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.StaxFilterConfiguration;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.WriterNodeContext;

/**
 *
 * @author maber01
 */
public class BubbleSplitter
{
  final Path sourcePath;
  final Path studentBasePath;
  final List<String> interestingStudentIds;

  final XMLOutputFactory writeFactory = XMLOutputFactory.newInstance();
  final XMLInputFactory readerFactory = XMLInputFactory.newInstance();

  public BubbleSplitter( Path sourcePath, Path studentBasePath, List<String> interestingStudentIds )
  {
    this.sourcePath = sourcePath;
    this.studentBasePath = studentBasePath;
    this.interestingStudentIds = interestingStudentIds;
  }
  
  public void process() throws IOException, XMLStreamException
  {    
    for ( String sid : interestingStudentIds )
    {
      Path sPath = this.studentBasePath.resolve( "student_" + sid + "_redacted.xml" );
      try ( InputStream in = Files.newInputStream( sourcePath );
            FileWriter fileWriter = new FileWriter( sPath.toFile() );
              )
      {
        processOneStudent( sid, in, fileWriter );
      }
    }
  }
  
  void processOneStudent( String sid, InputStream in, FileWriter fileWriter ) throws XMLStreamException, IOException
  {
    XMLEventReader reader = readerFactory.createXMLEventReader( in );
    XMLStreamWriter writer = writeFactory.createXMLStreamWriter( fileWriter );
    StaxFilterConfiguration config = new StaxFilterConfiguration();
    BubbleSplitterContext context = new BubbleSplitterContext( sid );
    config.outputFragment = true;
    FullFilterContext<BubbleSplitterContext,ReaderNodeContext,WriterNodeContext> ffc 
            = new FullFilterContext<>( context );
    
    SplitterFilter filter = new SplitterFilter( reader, writer, ffc, config );
    
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeCharacters( "\n" );
//    writer.writeStartElement( "bbcarv", "archive", Names.BCARVNAMESPACE );
//    writer.writeNamespace( "bbcarv", Names.BCARVNAMESPACE );
//    writer.writeCharacters( "\n" );
    filter.process();
//    writer.writeEndElement();
//    writer.writeCharacters( "\n" );
    writer.writeEndDocument();
    writer.writeCharacters( "\n" );
  }
  
  
}
