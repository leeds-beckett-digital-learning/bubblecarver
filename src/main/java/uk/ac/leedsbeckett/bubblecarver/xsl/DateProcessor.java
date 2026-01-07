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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author maber01
 */
public class DateProcessor
{
  private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern( "yyyy-MM-dd kk:mm:ss zzz" );
  public static String test( String in )
  {
    return "*****" + in + "*****";
  }
  
  public static String epoch( String in )
  {
    try
    {
      ZonedDateTime zdt = ZonedDateTime.parse( in, dtf );
      return Long.toString( zdt.toEpochSecond() );
    }
    catch ( Throwable t )
    {
      return "";
    }
  }
}
