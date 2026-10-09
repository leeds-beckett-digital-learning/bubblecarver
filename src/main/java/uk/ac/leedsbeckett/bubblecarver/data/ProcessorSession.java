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

import java.nio.file.Path;
import uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper;
import uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor;
import uk.ac.leedsbeckett.bubblecarver.xsl.GeneralMappingHelper;
import uk.ac.leedsbeckett.bubblecarver.xsl.ModuleUserInfoMapper;

/**
 *
 * @author maber01
 */
public class ProcessorSession
{
  public Path processingdir = null;
  public Path root = null;
  public DataLookupHelper datalookuphelper = null;
  public ModuleUserInfoMapper mapper = null;
  public FileProcessor fp = null;
  public GeneralMappingHelper generalmapper = null;
  
  public void release()
  {
    if ( datalookuphelper != null )
      datalookuphelper.release();
    if ( mapper != null )
      mapper.release();
    if ( fp != null )
      fp.release();
    if ( generalmapper != null )
      generalmapper.release();
  }
}
