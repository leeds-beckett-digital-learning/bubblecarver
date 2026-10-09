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

/**
 *
 * @author maber01
 */
public class ModuleUserInfo
{
  
  private final String uid;
  private String sid = null;
  private String mid = null;

  public ModuleUserInfo( String uid )
  {
    this.uid = uid;
  }
  
  public ModuleUserInfo( String uid, String sid )
  {
    this.uid = uid;
    this.sid = sid;
  }

  public String getUid()
  {
    return uid;
  }

  public String getSid()
  {
    return sid;
  }

  public void setSid( String sid )
  {
    this.sid = sid;
  }
  
  public String getMid()
  {
    return mid;
  }

  public void setMid( String mid )
  {
    this.mid = mid;
  }

  @Override
  public String toString()
  {
    return "ModuleUserInfo{" + "uid=" + uid + ", sid=" + sid + ", mid=" + mid + '}';
  }
}
