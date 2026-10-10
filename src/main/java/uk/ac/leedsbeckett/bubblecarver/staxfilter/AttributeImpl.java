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
package uk.ac.leedsbeckett.bubblecarver.staxfilter;

import java.io.Writer;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

/**
 *
 * @author maber01
 */
public class AttributeImpl implements Attribute
{
  final QName name;
  final String value;

  public AttributeImpl( QName name, String value) {
    this.name = name;
    this.value = value;
  }
  
  @Override
  public QName getName() {
    return name;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String getDTDType() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean isSpecified() {
    return true;
  }

  @Override
  public int getEventType() {
    return XMLStreamConstants.ATTRIBUTE;
  }

  @Override
  public Location getLocation() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean isStartElement() {
    return false;
  }

  @Override
  public boolean isAttribute()
  {
    return true;
  }

  @Override
  public boolean isNamespace() {
    return false;
  }

  @Override
  public boolean isEndElement() {
    return false;
  }

  @Override
  public boolean isEntityReference() {
    return false;
  }

  @Override
  public boolean isProcessingInstruction() {
    return false;
  }

  @Override
  public boolean isCharacters() {
    return false;
  }

  @Override
  public boolean isStartDocument() {
    return false;
  }

  @Override
  public boolean isEndDocument() {
    return false;
  }

  @Override
  public StartElement asStartElement() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public EndElement asEndElement() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Characters asCharacters() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public QName getSchemaType() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
}
