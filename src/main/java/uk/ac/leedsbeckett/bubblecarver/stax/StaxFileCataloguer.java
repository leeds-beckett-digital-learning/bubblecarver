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
package uk.ac.leedsbeckett.bubblecarver.stax;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.leedsbeckett.bubblecarver.combiner.Names;

/**
 * For each file
 * 
 * @author maber01
 */
public class StaxFileCataloguer
{
  private final Path base;
  private final String basepath;
  private XMLStreamWriter writer;
  
  public StaxFileCataloguer( Path base )
  {
    this.base = base;
    this.basepath = base.toString();
  }
  
  public void writeCatalogue( XMLStreamWriter writer ) throws XMLStreamException, IOException
  {
    writeCatalogue( writer, true );
  }
  
  public void writeCatalogue( XMLStreamWriter writer, boolean fragment ) throws XMLStreamException, IOException
  {
    this.writer = writer;
    if ( !fragment )
      writer.writeStartDocument("UTF-8", "1.0");
    writer.writeStartElement( Names.CATALOGUENAMESPACE, "catalogue" );
    if ( !fragment )
      writer.writeNamespace( "cat", Names.CATALOGUENAMESPACE );
    // This may be useful because it will include the location of
    // the zip archive if (as expected) that is the source of
    // the (java) file system.
    writer.writeAttribute( "uri", base.toUri().toString() );
    // Will be '/' if reading a zip file but could be
    // longer path if reading an unpacked zip file in a directory
    writer.writeAttribute( "base", base.toString() );
    
    PathProcessor pp = new PathProcessor();
    Stream<Path> streamSearch = Files.find( base, Integer.MAX_VALUE, (path,atts) -> atts.isRegularFile() );
    if ( streamSearch != null )
      streamSearch.forEach( pp );
    if ( pp.getException() != null )
      throw pp.getException();
    
    if ( !fragment )
      writer.writeEndDocument();
  }
  
  class PathProcessor implements Consumer<Path>
  {
    XMLStreamException exception = null;
    @Override
    public void accept( Path t )
    {
      if ( exception != null )
        return;
      
      if ( !Files.isRegularFile( t ) )
        return;
      // Only output files - not directories
      
      try
      {
        String path = t.toString();
        String relpath = path.substring( basepath.length() );
        writer.writeEmptyElement( Names.CATALOGUENAMESPACE, "file" );
        writer.writeAttribute( Names.PATHATTRIBUTE.getLocalPart(), relpath );
      }
      catch (XMLStreamException ex)
      {
        exception = ex;
      }
    }

    public XMLStreamException getException()
    {
      return exception;
    }
  }
  
}
