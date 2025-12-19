/*
 * Copyright 2025 maber01.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this zipdir except in compliance with the License.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author maber01
 */
public class Archive
{
  public static final String NS_BB_PACKAGE = "http://www.blackboard.com/content-packaging/";
  
  DocumentBuilder builder;
  XPath xPath = XPathFactory.newInstance().newXPath();

  File zipdir;
  File reportdir;
  Document manifestDocument;
  TreeModel manifestTreeModel;
  Document courseSettingsDocument;
  String courseSettingsCourseId=null;
  String courseSettingsTitle=null;
  TreeModel courseSettingsTreeModel;
  Document usersDocument;
  TreeModel usersTreeModel;
  Document membershipDocument;
  TreeModel membershipTreeModel;
  Document gradebookDocument;
  TreeModel gradebookTreeModel;
  HashMap<String,GradebookCategory> gradebookCategoryMap = new HashMap<>();
  
  ArrayList<Resource> attemptFilesResource   = new ArrayList<>();
  ArrayList<TreeModel> attemptFilesTreeModel = new ArrayList<>();

  ArrayList<Element> outdefList = new ArrayList<>();
  String userHtmlReport;
  
  public Archive( File file, File reportdir )
  {
    this.zipdir = file;
    this.reportdir = reportdir;
  }

  public Document getManifestDocument()
  {
    return manifestDocument;
  }
  
  public void load() throws ArchiveException
  {
    File manifestfile;
    manifestTreeModel = null;
    courseSettingsTreeModel = null;
    usersTreeModel = null;
    membershipTreeModel = null;
    gradebookTreeModel = null;
    attemptFilesTreeModel.clear();
    userHtmlReport = null;
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware( true );
      builder = factory.newDocumentBuilder();
      manifestfile = new File( zipdir, "imsmanifest.xml" );
      if ( !manifestfile.exists() )
        throw new ArchiveException( "The ZIP file does not contain an IMS manifest and so doesn't look like a BBL archive." );
      manifestDocument = loadXMLDocument( manifestfile );
      courseSettingsDocument = loadXMLDocument( findCourseSettings() );
      if ( courseSettingsDocument != null )
      {
        courseSettingsCourseId  = xPathString( "/COURSE/COURSEID/@value", courseSettingsDocument );
        courseSettingsTitle     = xPathString( "/COURSE/TITLE/@value", courseSettingsDocument );
      }
      usersDocument = loadXMLDocument( findUsers() );
      membershipDocument = loadXMLDocument( findMembership() );
      gradebookDocument = loadXMLDocument( findGradebook() );
      loadGradebookCategories( gradebookDocument );
      attemptFilesResource = findAttemptFilesList();
      for ( Resource r : attemptFilesResource )
        r.setDocument( loadXMLDocument( r ) );
    }
    catch ( IOException | ParserConfigurationException | SAXException | XPathExpressionException ex )
    {
      Logger.getLogger( Archive.class.getName() ).log( Level.SEVERE, null, ex );
      throw new ArchiveException( "IO problem loading archive: " + ex.getMessage(), ex );
    }
  }
  
  public void loadTrees()
  {
    manifestTreeModel = loadTreeModel( manifestDocument );
    if ( courseSettingsDocument != null )
      courseSettingsTreeModel = loadTreeModel( courseSettingsDocument );
    if ( usersDocument != null )
      usersTreeModel = loadTreeModel( usersDocument );
    if ( membershipDocument != null )
      membershipTreeModel = loadTreeModel( membershipDocument );
    if ( gradebookDocument != null )
      gradebookTreeModel = loadTreeModel( gradebookDocument );
    for ( Resource r : attemptFilesResource )
      attemptFilesTreeModel.add( loadTreeModel( r.getDocument() ) );    
  }

  public Resource findCourseSettings() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'course/x-bb-coursesetting']", false );
  }

  public Resource findUsers() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'course/x-bb-user']", false );
  }

  public Resource findMembership() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'membership/x-bb-coursemembership']", false );
  }
  
  public Resource findGradebook() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'course/x-bb-gradebook']", false );
  }
  
  public ArrayList<Resource> findAttemptFilesList() throws XPathExpressionException, ArchiveException
  {
    return findEntries( "/manifest/resources/resource[@type = 'course/x-bb-attemptfiles']" );
  }
  
  public Resource findEntry( String expression, boolean needed ) throws XPathExpressionException, ArchiveException
  {
    NodeList nodeList = xPathNodeList( expression, manifestDocument );
    if ( !needed && nodeList.getLength() == 0 )
      return null;
    if ( nodeList.getLength() == 0 )
      throw new ArchiveException( "Archive doesn't contain data file that matches " + expression + "." );
    Element resource = (Element)nodeList.item( 0 );
    String filename = resource.getAttributeNS( NS_BB_PACKAGE, "file" );
    if ( filename == null || filename.trim().length() == 0 )
      throw new ArchiveException( "Archive's entry that matches " + expression + " does not have a filename." );
    File file = new File( this.zipdir, filename );
    if ( !file.exists() )
      throw new ArchiveException( "Archive's entry named " + filename + " does not exist in unzipped archive." );
    return new Resource( 
            resource.getAttributeNS( "http://www.w3.org/XML/1998/namespace", "base" ), 
            file );
  }

  public ArrayList<Resource> findEntries( String expression ) throws XPathExpressionException, ArchiveException
  {
    ArrayList<Resource> list = new ArrayList<>();
    NodeList nodeList = (NodeList) xPath.compile( expression ).evaluate( manifestDocument, XPathConstants.NODESET);
    for ( int i=0; i<nodeList.getLength(); i++ )
    {
      Element resource = (Element)nodeList.item( i );
      String filename = resource.getAttributeNS( NS_BB_PACKAGE, "file" );
      if ( filename == null || filename.trim().length() == 0 )
        throw new ArchiveException( "Archive's entry that matches " + expression + " does not have a filename." );
      File file = new File( this.zipdir, filename );
      if ( !file.exists() )
        throw new ArchiveException( "Archive's entry named " + filename + " does not exist in unzipped archive." );
      list.add( 
          new Resource( resource.getAttributeNS( "http://www.w3.org/XML/1998/namespace", "base" ), 
          file ) );
    }
    return list;
  }


  void loadGradebookCategories( Document gradebookDocument ) throws XPathExpressionException
  {
    NodeList nodeList = (NodeList) xPath.compile( "/GRADEBOOK/CATEGORIES/CATEGORY" ).evaluate( gradebookDocument, XPathConstants.NODESET);
    for ( int i=0; i< nodeList.getLength(); i++ )
    {  
      Element e = (Element)nodeList.item( i );
      String id = e.getAttribute( "id" );
      String title = xPathString( "TITLE/@value", e );
      gradebookCategoryMap.put( id, new GradebookCategory( id, title ) );
    }
  }

  
  public TreeModel getManifestTreeModel()
  {
    return manifestTreeModel;
  }

  public TreeModel getCourseSettingsTreeModel()
  {
    return courseSettingsTreeModel;
  }

  public TreeModel getUsersTreeModel()
  {
    return usersTreeModel;
  }

  public TreeModel getMembershipTreeModel()
  {
    return membershipTreeModel;
  }  

  public TreeModel getGradebookTreeModel()
  {
    return gradebookTreeModel;
  }

  public List<TreeModel> getAttemptFilesTreeModel()
  {
    return attemptFilesTreeModel;
  }

  public String getUserHtmlReport()
  {
    return userHtmlReport;
  }
  
  void saveUserHtmlReport() throws IOException
  {
    long t = System.currentTimeMillis();
    File rfile = new File( reportdir, "report_ " + t + ".html" );
    try ( FileWriter writer = new FileWriter( rfile ) )
    {
      writer.write( userHtmlReport );
    }
  }

  public File getUserDir( File usersDir, String sid )
  {
    StringBuilder sb = new StringBuilder();
    for ( int i=sid.length(); i<8; i++ )
      sb.append( "_" );
    sb.append( sid );
    String longsid = sb.toString();
    File userDir = usersDir;
    for ( int i=2; i<8; i+=2 )
      userDir = new File( userDir, longsid.substring( 0, i ) );
    return new File( userDir, sid );
  }
  
  public void buildUserHtmlReport()
  {
    try
    {
      outdefList.clear();
      
      StringBuilder sb = new StringBuilder();
      sb.append( "<!DOCTYPE html>\n" );
      sb.append( "<html lang=\"en\">\n" );
      sb.append( "  <head>\n" );
      sb.append( "  <style>\n" );
      sb.append( "    .familyName { font-variant-caps: small-caps; }" );
      sb.append( "  </style>\n" );
      sb.append( "  </head>\n" );
      sb.append( "  <body>\n" );
      sb.append( "    <h1>BBL Archive Scan User Report</h1>\n" );
      sb.append( "    <h2>" );
      sb.append(zipdir.getAbsolutePath() );
      sb.append( "</h2>\n" );
      
      // Only outcomedefinitions that have outcomes included.
      NodeList nodeListG = (NodeList)xPath.compile( "/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION[count(OUTCOMES/OUTCOME)!=0]" ).evaluate( gradebookDocument, XPathConstants.NODESET );
      for ( int i=0; i<nodeListG.getLength(); i++ )
      {
        outdefList.add( (Element)nodeListG.item( i ) );
      }
      outdefList.sort( new Comparator<Element>(){
        @Override
        public int compare( Element o1, Element o2 )
        {
          try
          {
            Number n1 = (Number)xPath.compile( "POSITION/@value" ).evaluate( o1, XPathConstants.NUMBER );
            Number n2 = (Number)xPath.compile( "POSITION/@value" ).evaluate( o2, XPathConstants.NUMBER );
            if ( n1==null && n2==null )
              return 0;
            if ( n1 == null ) n1 = 0.0;
            if ( n2 == null ) n2 = 0.0;
            return Double.compare( n1.doubleValue(), n2.doubleValue() );
          }
          catch ( XPathExpressionException ex )
          {
          }
          return 0;
        }
      } );
      
      sb.append( "<ol>\n" );
      for ( int i=0; i<outdefList.size(); i++ )
      {
        Element e = outdefList.get( i );
        sb.append( "<li>" );
        String title = (String)xPath.compile( "TITLE/@value"    ).evaluate( e, XPathConstants.STRING );
        sb.append( title );
        sb.append( "</li>\n" );
      }
      sb.append( "</ol>\n" );
      
      ArrayList<Element> users = new ArrayList<>();
      if ( usersDocument != null )
      {
        NodeList nodeList = (NodeList) xPath.compile( "/USERS/USER" ).evaluate( usersDocument, XPathConstants.NODESET);
        for ( int i=0; i<nodeList.getLength(); i++ )
          users.add( (Element)nodeList.item( i ) );
      }
      users.sort( new Comparator<Element>() {
        @Override
        public int compare( Element o1, Element o2 )
        {
          String[] xpa = { "NAMES/FAMILY/@value", "NAMES/GIVEN/@value", "STUDENTID/@value" };
          String s1;
          String s2;
          int comparison;
          try
          {
            for ( String xp : xpa )
            {
              s1 = (String) xPath.compile( xp ).evaluate( o1, XPathConstants.STRING );
              s2 = (String) xPath.compile( xp ).evaluate( o2, XPathConstants.STRING );
              if ( s1 == null ) s1 = "";
              if ( s2 == null ) s2 = "";
              comparison = s1.compareTo( s2 );
              if ( comparison != 0 )
                return comparison;
            }
            return 0;
          }
          catch ( XPathExpressionException ex )
          {
            return 0;
          }
        }
      } );
      
      for ( Element user : users )
      {
        String id = user.getAttribute( "id" );
        if ( membershipDocument == null )
          continue;
        Element membership = (Element)xPath.compile( "/COURSEMEMBERSHIPS/COURSEMEMBERSHIP[USERID/@value = '" + id + "']" ).evaluate( membershipDocument, XPathConstants.NODE );
        if ( membership == null )
          continue;
        String role = (String)xPath.compile( "ROLE/@value"    ).evaluate( membership, XPathConstants.STRING );
        if ( !"STUDENT".equals( role ) )
          continue;
        
        String sid = (String) xPath.compile( "STUDENTID/@value" ).evaluate( user, XPathConstants.STRING );
        String familyname = (String) xPath.compile( "NAMES/FAMILY/@value" ).evaluate( user, XPathConstants.STRING );
        if ( familyname == null || familyname.length() == 0 )
          familyname = "UNKNOWN_SURNAME";
        if ( sid != null && sid.length() > 0 )
        {
          File usersDir = new File( reportdir, "students" );
          if ( !usersDir.exists() )
            usersDir.mkdir();
          File userDir = getUserDir( usersDir, sid );
          if ( !userDir.exists() )
            userDir.mkdirs();
          
          StringBuilder userSb = new StringBuilder();
          appendMemberReport( userDir, userSb, user, membership );
          File userFile = new File( userDir, sid + "___" + familyname + "___" + zipdir.getName() + ".html" );
          try ( FileWriter fw = new FileWriter( userFile ) )
          {
            fw.write( "<html>\n<head>\n<link href=\"../../../../../report.css\" rel=\"stylesheet\" />\n</head><body>" );
            fw.write( userSb.toString() );
            fw.write( "</body>\n</html>\n" );
          }
          sb.append( userSb );
        }
      }
      
      sb.append( "  </body>\n" );
      sb.append( "</html>\n" );
      userHtmlReport = sb.toString();
      saveUserHtmlReport();
    }
    catch ( IOException | XPathExpressionException ex )
    {
      Logger.getLogger( Archive.class.getName() ).log( Level.SEVERE, null, ex );
      userHtmlReport = null;
    }
  }
  
  void appendMemberReport( File userDir, StringBuilder sb, Element user, Element membership ) throws XPathExpressionException, IOException
  {
    String id = user.getAttribute( "id" );
    String sid =        (String)xPath.compile( "STUDENTID/@value"    ).evaluate( user, XPathConstants.STRING );
    String familyName = (String)xPath.compile( "NAMES/FAMILY/@value" ).evaluate( user, XPathConstants.STRING );
    String givenName =  (String)xPath.compile( "NAMES/GIVEN/@value"  ).evaluate( user, XPathConstants.STRING );

    
    String mid = membership.getAttribute( "id" );
    sb.append( "<h1 id=\"" );
    sb.append( id );
    sb.append( "\">" );
    sb.append( courseSettingsCourseId );
    sb.append( "<br/>" );
    sb.append( courseSettingsTitle );
    sb.append( "<br/>" );
    sb.append( sid );
    sb.append( " - " );
    sb.append( givenName );
    sb.append( " <span class=\"familyName\">" );
    sb.append( familyName );
    sb.append( "</span></h1>\n" );
    sb.append( "<div class=\"userReport\">\n" );

    for ( int i=0; i<outdefList.size(); i++ )
    {
      Element e = outdefList.get( i );
      sb.append( "<h2>" );
      String title = (String)xPath.compile( "TITLE/@value"    ).evaluate( e, XPathConstants.STRING );
      sb.append( title );
      sb.append( "</h2>\n" );
      sb.append( "<div class=\"outcomeReport\">\n" );
      appendOutcomesReport( userDir, sb, e, mid );
      sb.append( "</div>\n" );
    }

    sb.append( "</div>" );
  }
  
  void appendOutcomesReport( File userDir, StringBuilder sb, Element e, String mid ) throws XPathExpressionException, IOException
  {
    Element outcome = (Element) xPath.compile( 
      "OUTCOMES/OUTCOME[COURSEMEMBERSHIPID/@value = '" + mid + "']"
        ).evaluate( e, XPathConstants.NODE );
    if ( outcome == null )
    {
      sb.append( "<p>No data</p>" );
      return;
    }
    String catId           = (String)xPath.compile( "../../CATEGORYID/@value" ).evaluate( outcome, XPathConstants.STRING );
    String hUrl            = (String)xPath.compile( "../../HANDLERURL/@value" ).evaluate( outcome, XPathConstants.STRING );
    GradebookCategory gbcat = this.gradebookCategoryMap.get( catId );


    sb.append( "<h3>" );
    if ( gbcat != null )
    {
      String cattitle = gbcat.getTitle();
      sb.append( "<br/>" );
      sb.append( cattitle );
    }
    if ( hUrl != null )
    {
      sb.append( "<br/>" );
      sb.append( hUrl );
    }
    sb.append( "</h3>\n" );
    appendAttemptReport( userDir, sb, outcome );
  }  

  void appendAttemptReport( File userDir, StringBuilder sb, Element outcomeElement ) throws XPathExpressionException, IOException
  {
    String id = outcomeElement.getAttribute( "id" );
    NodeList nodeList = (NodeList) xPath.compile( "ATTEMPTS/ATTEMPT").evaluate( outcomeElement, XPathConstants.NODESET);
    for ( int i=0; i<nodeList.getLength(); i++ )
    {
      Element attempt = (Element)nodeList.item( i );
      sb.append( "<h3>Attempt " );
      sb.append( attempt.getAttribute( "id" ) );
      sb.append( "</h3>\n" );
      sb.append( "<div class=\"attemptReport\">\n" );
      sb.append( "<table>\n" );
      appendAttemptProperty( sb, attempt, "Date Attempted",         "DATEATTEMPTED/@value"           );
      appendAttemptProperty( sb, attempt, "Status",                 "STATUS/@value"                  );
      appendAttemptProperty( sb, attempt, "Score",                  "SCORE/@value"                   );
      appendAttemptProperty( sb, attempt, "Grade",                  "GRADE/@value"                   );
      appendAttemptProperty( sb, attempt, "Textual Submission",     "STUDENTSUBMISSION/TEXT/text()"  );
      appendAttemptProperty( sb, attempt, "Feedback from Marker",   "INSTRUCTORCOMMENTS/TEXT/text()" );
      appendAttemptProperty( sb, attempt, "Marker's Private Notes", "INSTRUCTORNOTES/TEXT/text()"    );
      sb.append( "<tr><th>Attached Files</th><td>\n" );
      appendAttemptFilesReport( userDir, sb, attempt.getAttribute( "id" ) );
      sb.append( "</td></tr>\n</table>\n</div>\n" );
    }
  }

  void appendAttemptProperty( StringBuilder sb, Element e, String label, String xp ) throws XPathExpressionException
  {
    String value  = (String)xPath.compile( xp ).evaluate( e, XPathConstants.STRING );
    sb.append( "<tr><th>" );
    sb.append( label );
    sb.append( "</th><td>" );
    sb.append( value );
    sb.append( "</td></tr>\n" );    
  } 
  
  void appendAttemptFilesReport( File userDir, StringBuilder sb, String id ) throws XPathExpressionException, IOException
  {
    if ( attemptFilesResource == null || attemptFilesResource.isEmpty() )
      return;

    boolean started = false;
    for ( Resource r : attemptFilesResource )
    {
      NodeList nodeList = xPathNodeList( "/ATTEMPTFILES/ATTEMPTFILE[ATTEMPTID/@value = '" + id + "']", r.getDocument() );
      for ( int i=0; i<nodeList.getLength(); i++ )
      {
        Element af       = (Element)nodeList.item( i );
        String afid      = af.getAttribute( "id" );
        String name      = xPathString( "FILE/NAME/text()", af );
        String linkname  = xPathString( "FILE/LINKNAME/@value", af );
        
        String zippath = r.base + "/" + afid + "/" + name;
        String reportpath = "attemptfiles/" + zippath;
        File attemptfile = new File( zipdir, zippath );
        if ( attemptfile.exists() )
        {
          File outputFile = new File( userDir, "attemptfiles/" + zippath );
          File outputDir = outputFile.getParentFile();
          outputDir.mkdirs();
          try ( InputStream in = Files.newInputStream( attemptfile.toPath() );
                OutputStream out = Files.newOutputStream( outputFile.toPath() ) )
          {
            IOUtils.copy( in, out );
          }
        }
        
        if ( !started )
        {
          sb.append(  "<ul>\n" );
          started = true;
        }
        sb.append( "<li><a href =\"" );
        sb.append( reportpath );
        sb.append( "\">" );
        sb.append( linkname );
        sb.append( "</a>" );
        sb.append( "</li>\n" );
      }
    }
    if ( started )
      sb.append(  "</ul>\n" );
  }
  
  Document loadXMLDocument( Resource resource ) throws IOException, SAXException
  {
    if ( resource == null ) return null;
    if ( resource.getFile() == null ) return null;
    return loadXMLDocument( resource.getFile() );
  }
  
  Document loadXMLDocument( File file ) throws IOException, SAXException
  {
    try ( InputStream in = new FileInputStream( file ) )
    {
      Document doc = builder.parse( in );
      doc.getDocumentElement().normalize();
      return doc;
    }
  }

  
  
  TreeModel loadTreeModel( Document doc )
  {
    return new DefaultTreeModel( loadTreeModelNode( doc.getDocumentElement() ) );
  }
  
  DefaultMutableTreeNode loadTreeModelNode( Element e )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "<" );
    if ( e.getPrefix() != null && e.getPrefix().length() > 0 )
    {
      sb.append( e.getPrefix() );
      sb.append( ":" );
    }
    sb.append( e.getLocalName() );
    NamedNodeMap atts = e.getAttributes();
    for ( int i=0; i<atts.getLength(); i++ )
    {
      sb.append( " " );
      Attr a = (Attr)atts.item( i );
      if ( a.getPrefix() != null && a.getPrefix().length() > 0 )
      {
        sb.append( a.getPrefix() );
        sb.append( ":" );
      }
      sb.append( a.getLocalName() );
      sb.append( "=\"" );
      sb.append( a.getValue() );
      sb.append( "\"" );
    }
    sb.append( ">" );
    
    DefaultMutableTreeNode node = new DefaultMutableTreeNode( sb.toString() );
    NodeList children = e.getChildNodes();
    if ( children.getLength() == 1 &&
         ( children.item( 0 ).getNodeType() == Node.TEXT_NODE ||
           children.item( 0 ).getNodeType() == Node.CDATA_SECTION_NODE ) )
    {
      String t = children.item( 0 ).getTextContent();
      if ( t != null && t.trim().length() != 0 )
      {
        node.add( new DefaultMutableTreeNode( t ) );
        return node;
      }
    }
    for ( int i=0; i<children.getLength(); i++ )
    {
      if ( children.item( i ) instanceof Element )
      {
        Element child = (Element)children.item( i );
        node.add( loadTreeModelNode( child ) );
      }
    }

    return node;
  }

  private String xPathString( String expr, Object node ) throws XPathExpressionException
  {
      return (String)xPath.compile( expr ).evaluate( node, XPathConstants.STRING );
  }

  private NodeList xPathNodeList( String expr, Object node ) throws XPathExpressionException
  {
      return (NodeList)xPath.compile( expr ).evaluate( node, XPathConstants.NODESET );
  }
  
  
  public class Resource
  {
    private final String base;
    private final File file;
    private Document document = null;

    public Resource( String base, File file )
    {
      this.base = base;
      this.file = file;
    }

    public String getBase()
    {
      return base;
    }

    public File getFile()
    {
      return file;
    }

    public Document getDocument()
    {
      return document;
    }

    public void setDocument( Document document )
    {
      this.document = document;
    }
  }
  
  public class GradebookCategory
  {
    private final String id;
    private final String title;

    public GradebookCategory( String id, String title )
    {
      this.id = id;
      this.title = title;
    }

    public String getId()
    {
      return id;
    }

    public String getTitle()
    {
      return title;
    }
  }
  
}
