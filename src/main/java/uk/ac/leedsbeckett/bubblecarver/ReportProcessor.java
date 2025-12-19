/*
 * Copyright 2025 maber01.
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
package uk.ac.leedsbeckett.bubblecarver;

import java.io.File;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.TreeNode;
import uk.ac.leedsbeckett.bubblecarver.data.ArchiveDirectoryTreeNode;
import uk.ac.leedsbeckett.bubblecarver.data.FileData;

/**
 *
 * @author maber01
 */
public class ReportProcessor implements Runnable
{
  private final ArchiveDirectoryTreeNode root;
  private final File reportdir;
  
  public ReportProcessor(  File reportdir, ArchiveDirectoryTreeNode root )
  {
    this.reportdir = reportdir;
    this.root      = root;
  }
  
  public void start()
  {
    Thread thread = new Thread( this );
    thread.start();
  }
  
  @Override
  public void run()
  {
    try
    {
      Enumeration<TreeNode> e = root.depthFirstEnumeration();
      while ( e.hasMoreElements() )
      {
        ArchiveDirectoryTreeNode node = (ArchiveDirectoryTreeNode)e.nextElement();
        FileData fd = node.getFileData();
        if ( fd != null && fd.getFile().isFile() )
        {
          System.out.println( "ReportProcessor: " + fd.getFile().getName() );
          Archive a = new Archive( fd.getFile(), reportdir );
          a.load();
          a.buildUserHtmlReport();        
        }
      }
      System.out.println( "ReportProcessor: Complete" );
    }
    catch ( ArchiveException ex )
    {
      Logger.getLogger( ReportProcessor.class.getName() ).log( Level.SEVERE, null, ex );
    }
  }
}
