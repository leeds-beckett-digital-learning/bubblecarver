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

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.*;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.*;

/**
 *
 * @author maber01
 */
public class BubbleCombiner
{
  final BubbleCombinerContext context;
  GeneralMap generalMap;
  
  public BubbleCombiner( Path inputPath, Path outputPath)
  {
    this.context = new BubbleCombinerContext( inputPath, outputPath );
  }
  
  public void process() throws IOException, XMLStreamException
  {    
    try ( InputStream in = Files.newInputStream( context.inputPath );
          FileWriter fileWriter = new FileWriter( context.outputPath.toFile() );
            )
    {
      doIt( in, fileWriter );
    }
  }
  
  public void doIt( InputStream in, FileWriter fileWriter ) throws XMLStreamException, IOException
  {
    XMLEventReader reader = context.getReaderFactory().createXMLEventReader( in );
    XMLStreamWriter writer = context.getWriteFactory().createXMLStreamWriter( fileWriter );
    StaxFilterConfiguration config = new StaxFilterConfiguration();
    config.outputFragment = true;
    FullFilterContext<BubbleCombinerContext,ReaderNodeContext,WriterNodeContext> ffc 
            = new FullFilterContext<>( context );
    
    MainFilter filter = new MainFilter( reader, writer, ffc, config );
    
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeCharacters( "\n" );
    writer.writeStartElement( "bbcarv", "archive", Names.BCARVNAMESPACE );
    writer.writeCharacters( "\n" );
    filter.process();
    writer.writeEndElement();
    writer.writeCharacters( "\n" );
    writer.writeEndDocument();
    writer.writeCharacters( "\n" );
    generalMap = filter.getContext().getFilterContext().getGeneralMap();
  }
  
  public GeneralMap getGeneralMap()
  {
    return generalMap;
  }
}
