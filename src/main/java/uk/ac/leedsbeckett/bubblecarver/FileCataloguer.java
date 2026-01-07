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
package uk.ac.leedsbeckett.bubblecarver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author maber01
 */
public class FileCataloguer
{
  public static final String NS = "http://www.leedsbeckett.ac.uk/bubblecarver/catalogue";
  private static final Object syn = new Object();
  private static DocumentBuilder docbuilder = null;
  
  
  private final Path base;
  private Document doc;
  private Element root;
        
  public FileCataloguer( Path base )
  {
    this.base = base;
  }
  
  public Document getCatalogue() throws ParserConfigurationException, IOException
  {
    if ( doc != null ) return doc;
    
    synchronized ( syn ) // All instances using same docbuilder
    {
      if ( docbuilder == null )
        docbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      doc = docbuilder.newDocument();
    }
    
    root = doc.createElementNS( NS, "cat:catalogue" );
    doc.appendChild( root );
    
    Stream<Path> streamSearch = Files.find( base, Integer.MAX_VALUE, (path,atts) -> atts.isRegularFile() );
    if ( streamSearch != null )
      streamSearch.forEach( new PathProcessor() );
    
    return doc;
  }
  
  public class PathProcessor implements Consumer<Path>
  {
    @Override
    public void accept( Path t )
    {      
      if ( !Files.isRegularFile( t ) )
        return;
      Path rel = base.relativize( t );
      Element f = doc.createElementNS( NS, "cat:file" );
      f.setAttribute( "path", rel.toString() );
      root.appendChild( f );
      for ( Path part : rel )
      {
        Element p = doc.createElementNS( NS, "cat:dir" );
        p.setAttribute( "name", part.toString() );
        f.appendChild( p );
      }
    }
  }
  
}
