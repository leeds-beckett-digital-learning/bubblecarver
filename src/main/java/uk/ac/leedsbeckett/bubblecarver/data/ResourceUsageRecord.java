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

import java.util.HashSet;

/**
 *
 * @author maber01
 */
public class ResourceUsageRecord
{
  final HashSet<String> gidSet = new HashSet<>();
  final HashSet<String> sidSet = new HashSet<>();
  public void addGid( String gid )
  {
    gidSet.add( gid );
  }
  public void addSid( String sid )
  {
    sidSet.add( sid );
  }
  public boolean isGidUser( String gid )
  {
    return gidSet.contains( gid );
  }
  public boolean isSidUser( String sid )
  {
    return sidSet.contains( sid );
  }
}
