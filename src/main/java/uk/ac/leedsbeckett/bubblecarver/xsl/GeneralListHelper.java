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
package uk.ac.leedsbeckett.bubblecarver.xsl;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author maber01
 */
public class GeneralListHelper extends XSLHelper
{  
  public static String add( String helperid, String value )
  {
    GeneralListHelper helper = get(GeneralListHelper.class, helperid );
    return helper.add( value );
  }

  private final ArrayList<String> list = new ArrayList<>();
  
  public synchronized String add( String value )
  {
    list.add( value );
    return value;
  }
  
  public synchronized List<String> getList()
  {
    return list;
  }
}
