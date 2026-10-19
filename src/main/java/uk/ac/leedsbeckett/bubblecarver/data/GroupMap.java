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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author maber01
 */
public class GroupMap
{
  final HashMap<String,Entry> map = new HashMap<>();
  
  final HashMap<String,HashSet<String>> groupMap = new HashMap<>();
  
  public void setGroupId( String groupMembershipId, String groupId )
  {
    Entry entry = getOrCreateEntry( groupMembershipId );
    entry.setGroupId( groupId );
  }
  
  public void setCourseMembershipId( String groupMembershipId, String courseMembershipId )
  {
    Entry entry = getOrCreateEntry( groupMembershipId );
    entry.setCourseMembershipId( courseMembershipId );    
  }
  
  public void rebuild( GeneralMap genMap )
  {
    for ( Entry entry : map.values() )
    {
      if ( entry.getGroupId() == null || entry.getCourseMembershipId() == null )
        continue;
      String userId = genMap.get( "member->user", entry.getCourseMembershipId() );
      String studentId = genMap.get( "user->sid", userId );
      entry.setStudentId( studentId );
      HashSet<String> group = getOrCreateGroupList( entry.getGroupId() );
      group.add( studentId );
    }
  }
  
  public boolean isMemberOfGroup( String groupId, String studentId )
  {
    HashSet<String> group = groupMap.get( groupId );
    return group != null && group.contains( studentId );
  }
  
  public String getGroupOfMember( String groupMembershipId )
  {
    Entry entry = map.get( groupMembershipId );
    if ( entry == null ) return null;
    return entry.getGroupId();
  }
  
  Entry getOrCreateEntry( String groupMembershipId )
  {
    Entry entry = map.get( groupMembershipId );
    if ( entry == null )
    {
      entry = new Entry( groupMembershipId );
      map.put( groupMembershipId, entry );
    }
    return entry;
  }
  
  HashSet<String> getOrCreateGroupList( String groupId )
  {
    HashSet<String> group = groupMap.get( groupId );
    if ( group == null )
    {
      group = new HashSet<>();
      groupMap.put( groupId, group );
    }
    return group;
  }
  
  public class Entry
  {
    final String groupMembershipId;
    String groupId = null;
    String courseMembershipId = null;
    String studentId = null;

    public Entry(String groupMembershipId )
    {
      this.groupMembershipId = groupMembershipId;
    }

    public String getGroupMembershipId()
    {
      return groupMembershipId;
    }

    public String getGroupId()
    {
      return groupId;
    }

    public void setGroupId(String groupId)
    {
      this.groupId = groupId;
    }

    public String getCourseMembershipId()
    {
      return courseMembershipId;
    }

    public void setCourseMembershipId(String courseMembershipId)
    {
      this.courseMembershipId = courseMembershipId;
    }

    public String getStudentId()
    {
      return studentId;
    }

    public void setStudentId(String studentId)
    {
      this.studentId = studentId;
    }
  }
}
