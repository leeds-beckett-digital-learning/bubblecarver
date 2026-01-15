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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author maber01
 */
public class StringProcessor
{
  private static DecimalFormat scoreformat = new DecimalFormat( "0.0" );
  
  public static String replace( String src, String find, String replace )
  {
    return src.replace( find, replace );
  }
  
  public static String reformatScore( String original )
  {
    try
    {
      Number n = scoreformat.parse( original );
      return scoreformat.format( n );
    }
    catch ( ParseException ex )
    {
      return original;
    }
  }
}
