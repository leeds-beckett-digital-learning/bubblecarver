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

import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

/**
 *
 * @author maber01
 */
public class ElementWritePlan
{
  QName name = null;
  final ArrayList<Namespace> namespaceList = new ArrayList<>();
  final ArrayList<Attribute> attributeList = new ArrayList<>();
  
  public ElementWritePlan()
  {
  }
  
  public ElementWritePlan( QName name )
  {
    this.name = name;
  }
  
  public ElementWritePlan( StartElement event )
  {
    name = event.getName();
    for ( Iterator<Namespace> i = event.getNamespaces(); i.hasNext(); )
      namespaceList.add( i.next() );
    for ( Iterator<Attribute> i = event.getAttributes(); i.hasNext(); )
      attributeList.add( i.next() );
  }

  public QName getName()
  {
    return name;
  }

  public void setName( QName name )
  {
    this.name = name;
  }

  public ArrayList<Namespace> getNamespaceList()
  {
    return namespaceList;
  }

  public ArrayList<Attribute> getAttributeList()
  {
    return attributeList;
  }
  
  public Attribute getAttribute( QName name )
  {
    for ( Attribute a : attributeList )
    {
      // QName implements equals sensibly
      if ( name.equals( a.getName() ) )
        return a;
    }
    return null;
  }

  public String getAttributeValue( QName name )
  {
    Attribute a = getAttribute( name );
    if ( a == null ) return null;
    return a.getValue();
  }
}
