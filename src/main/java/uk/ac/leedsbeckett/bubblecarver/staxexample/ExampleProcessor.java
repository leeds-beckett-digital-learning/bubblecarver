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
package uk.ac.leedsbeckett.bubblecarver.staxexample;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.FilterContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.FullFilterContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.ReaderNodeContext;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.StaxFilterConfiguration;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.WriterNodeContext;

/**
 *
 * @author maber01
 */
public class ExampleProcessor
{
  final Path inputPath;
  final Path outputPath;

  public ExampleProcessor(Path inputPath, Path outputPath)
  {
    this.inputPath = inputPath;
    this.outputPath = outputPath;
  }
  
  public void process() throws IOException, XMLStreamException
  {    
    try ( InputStream in = Files.newInputStream( inputPath );
          FileWriter fileWriter = new FileWriter( outputPath.toFile() );
            )
    {
      doIt( in, fileWriter );
    }
  }
  
  public void doIt( InputStream in, FileWriter fileWriter ) throws XMLStreamException, IOException
  {
    final XMLOutputFactory writeFactory = XMLOutputFactory.newInstance();
    final XMLInputFactory readerFactory = XMLInputFactory.newInstance();
    XMLEventReader reader = readerFactory.createXMLEventReader( in );
    XMLStreamWriter writer = writeFactory.createXMLStreamWriter( fileWriter );
    StaxFilterConfiguration config = new StaxFilterConfiguration();
    FullFilterContext<FilterContext,ReaderNodeContext,WriterNodeContext> ffc = new FullFilterContext<>( new FilterContext() );
    
    ExampleStaxFilter filter = new ExampleStaxFilter( reader, writer, ffc, config );
    filter.process();
  }
}
