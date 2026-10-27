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
package uk.ac.leedsbeckett.bubblecarver.reporter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.leedsbeckett.bubblecarver.xsl.GeneralListHelper;

/**
 *
 * @author maber01
 */
public class BubbleReporter
{
  private static DocumentBuilder builder;
  private final Transformer transformStudentReport;
  

  public BubbleReporter( Transformer transformStudentReport )
  {
    this.transformStudentReport       = transformStudentReport;
  }
  
  public void processStudent( Path t ) throws IOException, TransformerException, ParserConfigurationException, SAXException
  {
    if ( builder == null )
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware( true );
      builder = factory.newDocumentBuilder();
    }
    
    Path report = t.getParent().resolve( "report.html" );
    GeneralListHelper listHelper = new GeneralListHelper();
    
    // Same source document is used multiple times so good idea to load
    // whole thing into memory as DOM document.
    Document doc = builder.parse( t.toFile() );
    //doc.getDocumentElement().normalize();
    
    try ( OutputStream out = Files.newOutputStream( report ) )
    {
      DOMSource source = new DOMSource( doc );
      StreamResult result = new StreamResult( out );
      transformStudentReport.reset();
      transformStudentReport.clearParameters();
      transformStudentReport.setParameter( "list-helper-id", listHelper.getId() );
      transformStudentReport.transform( source, result );
    }
    
    for ( String item : listHelper.getList() )
    {
      System.out.println( "Gradebook line ID: " + item );
      Path columnreport = t.getParent().resolve( "report" + item + ".html" );
      try ( OutputStream out = Files.newOutputStream( columnreport ) )
      {
        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( out );
        transformStudentReport.reset();
        transformStudentReport.clearParameters();
        transformStudentReport.setParameter( "outcome-def-id", item );
        transformStudentReport.transform( source, result );
      }
    }
    listHelper.release();

    // Just in case this saved some memory on last use.
    transformStudentReport.reset();
  }
}
