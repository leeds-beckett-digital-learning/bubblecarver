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
package uk.ac.leedsbeckett.bubblecarver.data;

import org.w3c.dom.Element;

/**
 *
 * @author maber01
 */
public class ElementTreeData
{
  private final Element e;
  private final String text;

  public ElementTreeData( Element e, String text )
  {
    this.e = e;
    this.text = text;
  }

  @Override
  public String toString()
  {
    return text;
  }

  public Element getElement()
  {
    return e;
  }
}
