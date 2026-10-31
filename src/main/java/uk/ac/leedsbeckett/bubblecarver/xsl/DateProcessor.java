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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;

/**
 *
 * @author maber01
 */
public class DateProcessor
{
  private static final ZoneId zoneId = ZoneId.of( "Europe/London" );
  private static final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
  static
  {
    builder.appendPattern( "yyyy-MM-dd HH:mm:ss " );
    builder.appendZoneText( TextStyle.SHORT, Set.of( zoneId ) );
  }
  private static final DateTimeFormatter dtf = builder.toFormatter();

  
  public static String test( String in )
  {
    return "*****" + in + "*****";
  }
  
  public static String epoch( String in )
  {
    ZonedDateTime zdt = ZonedDateTime.parse( in, dtf );
    return Long.toString( zdt.toEpochSecond() );
  }
  
  public static String reverse( String epoch )
  {
    long t = Long.parseLong( epoch );
    ZonedDateTime zdt = ZonedDateTime.ofInstant( Instant.ofEpochSecond( t ), zoneId );
    return dtf.format( zdt );
  }

  public static String utc( String epoch )
  {
    long t = Long.parseLong( epoch );
    return Instant.ofEpochSecond( t ).toString();
  }
}
