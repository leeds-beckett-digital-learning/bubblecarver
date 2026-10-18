/*
 * Copyright 2026 maber01.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.leedsbeckett.bubblecarver.data;

import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author maber01
 */
public class WantedManifestResourceTypes
{
  static final String[] typeArray = {
//    "assessment/x-bb-qti-attempt",
//    "assessment/x-bb-qti-test",
    "course/x-bb-attemptembeddedfiles",
    "course/x-bb-attemptfiles",
//    "course/x-bb-availability-rule",
//    "course/x-bb-contenthandler",
//    "course/x-bb-courseassessment",
//    "course/x-bb-coursemessage",
//    "course/x-bb-coursemessagefolder",
    "course/x-bb-coursesetting",
//    "course/x-bb-crsrubricassocation",
//    "course/x-bb-crsrubriceval",
    "course/x-bb-gradebook",
    "course/x-bb-group",
    "course/x-bb-groupattemptfiles",
    "course/x-bb-groupupload",
//    "course/x-bb-review-status",
//    "course/x-bb-rubrics",
    "course/x-bb-user",
//    "course/x-bb-usertestoption",
//    "discussionboard/x-bb-usermsgstate",
    "membership/x-bb-coursemembership",
    "membership/x-bb-groupmembership",
//    "resource/x-bb-achievements",
//    "resource/x-bb-attendance",
//    "resource/x-bb-blog",
//    "resource/x-bb-document",
//    "resource/x-bb-retention",
//    "resource/x-bb-task",
//    "resource/x-bb-taskprogress",
//    "resource/x-bb-video-integration",
    "resource/x-bb-wiki",
//    "resource/x-plugin-scormengine"
  };
  
  static final HashSet<String> typeMap = new HashSet<>();
  static
  {
    typeMap.addAll( Arrays.asList( typeArray ) );
  }
  
  public static boolean isWanted( String type )
  {
    return typeMap.contains( type );
  }
  
  public static boolean isWeird( String type )
  {
    return "course/x-bb-availability-rule".equals( type ) ||
            "assessment/x-bb-qti-attempt".equals( type );
  }
}
