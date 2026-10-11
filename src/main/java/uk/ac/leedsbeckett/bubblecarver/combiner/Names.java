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

import javax.xml.namespace.QName;

/**
 *
 * @author maber01
 */
public class Names
{
  public final static String XMLNAMESPACE = "http://www.w3.org/XML/1998/namespace";
  public final static String BBNAMESPACE = "http://www.blackboard.com/content-packaging/";
  public final static String BCARVNAMESPACE = "http://www.leedsbeckett.ac.uk/bubblecarver";
  public final static QName XMLBASEATTRIBUTE = new QName( XMLNAMESPACE, "base" );
  public final static QName IDATTRIBUTE = new QName( "id" );
  public final static QName VALUEATTRIBUTE = new QName( "value" );
  public final static QName TYPEATTRIBUTE = new QName( "type" );
  public final static QName IDENTIFIERATTRIBUTE = new QName( "identifier" );
  public final static QName HREFATTRIBUTE = new QName( "href" );
  public final static QName BBFILEATTRIBUTE = new QName( BBNAMESPACE, "file" );
  public final static QName BCARVIDATTRIBUTE = new QName( BCARVNAMESPACE, "id" );
  public final static QName BCARVCONTENTIDATTRIBUTE = new QName( BCARVNAMESPACE, "contentid" );
  public final static QName BCARVMEMBERIDATTRIBUTE = new QName( BCARVNAMESPACE, "mid" );
  public final static QName BCARVUSERIDATTRIBUTE = new QName( BCARVNAMESPACE, "uid" );
  public final static QName BCARVSTUDENTIDATTRIBUTE = new QName( BCARVNAMESPACE, "sid" );
  public final static QName BCARVUUIDATTRIBUTE = new QName( BCARVNAMESPACE, "uuid" );
  public final static QName BCARVOUTCOMEATTRIBUTE = new QName( BCARVNAMESPACE, "outcome" );
}
