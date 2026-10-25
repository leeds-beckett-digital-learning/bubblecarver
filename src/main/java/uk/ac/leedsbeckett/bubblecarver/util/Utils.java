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
package uk.ac.leedsbeckett.bubblecarver.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *
 * @author maber01
 */
public class Utils
{
  private static final DateTimeFormatter dateTimeFormatter 
          = DateTimeFormatter.ofPattern( "yyyy_MM_dd__HH_mm_ss" );

  
  /**
   * Date and time format suitable for file names
   * @param instant
   * @return 
   */
  
  public static String toFileNameString( Instant instant )
  {
    // Format the UTC zone version of the instant
    return dateTimeFormatter.format( instant.atZone( ZoneId.of( "UTC" ) ) );
  }

  public static String getNowString()
  {
    // Format the UTC zone version of the instant
    return toFileNameString( Instant.now() );
  }
  
  private final static DateTimeFormatter propertiesDTFormatter = 
          DateTimeFormatter.ofPattern( "LLL dd HH:mm:ss yyyy", Locale.US );
  public static LocalDateTime parsePropertiesDateTime( String line )
  {
    // Split
    String[] bits = line.substring( 5 ).split( " " );
    // Join again without time zone:
    StringBuilder sb = new StringBuilder();
    sb.append( bits[0] ); // Month
    sb.append( " " );
    sb.append( bits[1] ); // Day of month
    sb.append( " " );
    sb.append( bits[2] ); // hh:mm:ss
    sb.append( " " );
    sb.append( bits[4] ); // years
    return LocalDateTime.parse( sb.toString(), propertiesDTFormatter );
  }
}
