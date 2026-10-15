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

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author maber01
 */
public class BetterIndentingXMLStreamWriter extends IndentingXMLStreamWriter
{
  boolean charactersPending=false;
  StringBuilder pending = new StringBuilder();
  
  int priorNodeType = -1;       // The node before string of character writes
  int currentNodeType = -1;     // The currently processing node
  int previousNodeType = -1;    // The node before
  
  public BetterIndentingXMLStreamWriter( XMLStreamWriter writer )
  {
    super(writer);
  }

  @Override
  public void writeCData(String data) throws XMLStreamException
  {
    throw new XMLStreamException( "Unable to handle CDATA at present." );
  }

  @Override
  public void writeCharacters(char[] text, int start, int len) throws XMLStreamException
  {
    writeCharacters( new String( text, start, len ) );
  }

  @Override
  public void writeCharacters(String text) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.CDATA;    
    if ( !charactersPending )
    {
      priorNodeType = previousNodeType;      
      charactersPending = true;
    }
    pending.append( text );
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeEndElement() throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.END_ELEMENT;
    writeConsolidatedCharacters();
    super.writeEndElement();
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeEmptyElement(String localName) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.END_ELEMENT;
    writeConsolidatedCharacters();
    super.writeEmptyElement(localName);
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.END_ELEMENT;
    writeConsolidatedCharacters();
    super.writeEmptyElement(prefix, localName, namespaceURI);
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.END_ELEMENT;
    writeConsolidatedCharacters();
    super.writeEmptyElement(namespaceURI, localName);
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.START_ELEMENT;
    writeConsolidatedCharacters();
    super.writeStartElement(prefix, localName, namespaceURI);
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.START_ELEMENT;
    writeConsolidatedCharacters();
    super.writeStartElement(namespaceURI, localName); 
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeStartElement(String localName) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.START_ELEMENT;
    writeConsolidatedCharacters();
    super.writeStartElement(localName);
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeStartDocument(String encoding, String version) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.START_DOCUMENT;
    writeConsolidatedCharacters();
    super.writeStartDocument(encoding, version); 
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeStartDocument(String version) throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.START_DOCUMENT;
    writeConsolidatedCharacters();
    super.writeStartDocument(version);
    previousNodeType = currentNodeType;
  }

  @Override
  public void writeStartDocument() throws XMLStreamException
  {
    currentNodeType = XMLStreamConstants.START_DOCUMENT;
    writeConsolidatedCharacters();
    super.writeStartDocument(); 
    previousNodeType = currentNodeType;
  }
  
  void writeConsolidatedCharacters() throws XMLStreamException
  {
    if ( charactersPending && currentNodeType != XMLStreamConstants.CHARACTERS )
    {
      String s = processString( pending.toString() );
      if ( s!=null && s.length() > 0 )
        super.writeCharacters( s );
      pending.setLength( 0 );
      charactersPending = false;
    }
  }
  
  String processString( String full )
  {
    // If text is leaf node so just trim and output
//    if (   priorNodeType == XMLStreamConstants.START_ELEMENT && 
//         currentNodeType == XMLStreamConstants.END_ELEMENT      )
//      return full.trim();

    // Otherwise trim and output
    return full.trim();
  }
}
