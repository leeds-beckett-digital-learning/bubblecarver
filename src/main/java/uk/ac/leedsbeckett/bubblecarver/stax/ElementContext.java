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

import java.util.HashMap;
import javax.xml.stream.events.StartElement;

/**
 *
 * @author maber01
 */
public class ElementContext
{  
  public StartElement element;
  public String path;
  public String mode;
  final HashMap<String, String> properties = new HashMap<>();

  public void putProperty( String name, String value )
  {
    properties.put( name, value );
  }

  public String getProperty( String name )
  {
    return properties.get( name );
  }
}
