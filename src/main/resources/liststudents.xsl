<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : liststudents.xsl
    Created on : 16 December 2025, 12:09
    Author     : maber01
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:bb="http://www.blackboard.com/content-packaging/"
  xmlns:redirect="http://xml.apache.org/xalan/redirect"
  xmlns:bbcarv="http://www.leedsbeckett.ac.uk/bubblecarver"
  xmlns:cat="http://www.leedsbeckett.ac.uk/bubblecarver/catalogue"
  xmlns:java="http://xml.apache.org/xalan/java"
  extension-element-prefixes="redirect java"
  exclude-result-prefixes="bb"
  version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="output-base-uri" select="'unknown path to nowhere'"/>
  <xsl:param name="file-processor-id" select="'unknown'"/>

  <!--
      This variable lists the MIME types of all the resources from the
      imsmanifest that we want to put in the student centred archive
      output. They are concatenated and delimited by new lines.
  -->
  <xsl:variable name="types">
    course/x-bb-coursesetting
    course/x-bb-user
    membership/x-bb-coursemembership
    course/x-bb-group
    membership/x-bb-groupmembership
    course/x-bb-gradebook
    course/x-bb-attemptfiles
    course/x-bb-attemptembeddedfiles
    resource/x-bb-wiki
  </xsl:variable>

  <!--.
      This variable joins together multiple resources from the imsmanifest
      and embeds the actual XML content from the files they reference.
      The result is not a node set so needs further processing.
  -->
  <xsl:variable name="unredactedfrag">
    <bbcarv:resources>
      <xsl:for-each select="/manifest/resources/resource[ contains( $types, @type ) ]">
        <bbcarv:resource type="{@type}" xml:base="{@xml:base}">
          <bbcarv:data>
            <xsl:copy-of select="document( ./@bb:file )"/>
          </bbcarv:data>          
          <xsl:for-each select="file">
            <bbcarv:file>
              <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
            </bbcarv:file>
          </xsl:for-each>
        </bbcarv:resource>
      </xsl:for-each>
      <xsl:copy-of select="document( 'catalogue:' )"/>
    </bbcarv:resources>
  </xsl:variable>

  <!--
      Converts the variable above into a node set.
  -->
  <xsl:variable name="unredacted" select="nodeset( $unredactedfrag )"/>
                
  

  <!-- 
      The entry point template that starts the processing of the imsmanifest
      input file. It iterates students found in the unredacted data and for
       each one outputs a redacted version of the data with other students'
      data removed.
  -->
  <xsl:template match="/">
    <xsl:text>
</xsl:text>
    <archive-log>
      <xsl:attribute name="archive"><xsl:value-of select="$unredacted/bbcarv:resources/bbcarv:resource/bbcarv:data/COURSE/COURSEID/@value"/></xsl:attribute>
      
      <xsl:for-each select="$unredacted/bbcarv:resources/bbcarv:resource/bbcarv:data/USERS/USER[string-length(STUDENTID/@value) &gt;= 8]">
        <xsl:variable name="uid" select="@id"/>
        <xsl:variable name="sid" select="STUDENTID/@value"/>
        <xsl:variable name="student-dir">
          <xsl:call-template name="make-student-uri">
            <xsl:with-param name="sid" select="$sid"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="student-data-file" select="concat($student-dir,'data.xml')"/>
        <xsl:variable name="memberid" select="$unredacted/bbcarv:resources/bbcarv:resource/bbcarv:data/COURSEMEMBERSHIPS/COURSEMEMBERSHIP/USERID[@value = $uid]/../@id"/>
        <xsl:variable name="student-rel-dir">
          <xsl:call-template name="make-student-relative-uri">
            <xsl:with-param name="sid" select="$sid"/>
          </xsl:call-template>
        </xsl:variable>
        
        <!-- 
              This variable puts the unredacted data into a tree with
              the current student's IDs near the root. This will allow
              any template to find the current student ID based purely
              on the context node. This is done because we need to use
              apply-templates and there is no mechanism to send parameters.
.        -->
        <xsl:variable name="unredactedparameterizedfrag">
          <bbcarv:studentandfiles>
            <bbcarv:student>
              <xsl:attribute name="id"><xsl:value-of select="$uid"/></xsl:attribute>
              <xsl:attribute name="memberid"><xsl:value-of select="$memberid"/></xsl:attribute>
              <xsl:attribute name="rel-uri"><xsl:value-of select="$student-rel-dir"/></xsl:attribute>
            </bbcarv:student>
            <xsl:copy-of select="$unredacted"/>
          </bbcarv:studentandfiles>
        </xsl:variable>
        
        <!-- Above converted to node set -->
        <xsl:variable name="unredactedparameterized" select="nodeset( $unredactedparameterizedfrag )"/>
        
        <student-log>
          <xsl:attribute name="student-file"><xsl:value-of select="$student-dir"/></xsl:attribute>
        </student-log>
        
        <!-- 
              Here the student file is created. The outermost 'student' element
              is output and then apply-templates with mode="redact" inserts a
              redacted version of the unredacted original data.
        -->
        <redirect:write file="{$student-data-file}">
          <xsl:text>
</xsl:text>
          <bbcarv:student sid="{$sid}" uid="{$uid}" memberid="{$memberid}">
            <xsl:apply-templates select="$unredactedparameterized/bbcarv:studentandfiles/bbcarv:resources" mode="redact"/>
          </bbcarv:student>
        </redirect:write>

        <redirect:write file="{concat($student-dir,'catalogue.xml')}">
          <xsl:text>
</xsl:text>
          <bbcarv:student sid="{$sid}" uid="{$uid}" memberid="{$memberid}">
            <xsl:apply-templates select="$unredactedparameterized/bbcarv:studentandfiles/bbcarv:resources/cat:catalogue" mode="catredact"/>
          </bbcarv:student>
        </redirect:write>
        
      </xsl:for-each>
    </archive-log>
  </xsl:template>


<!-- ====================================================================== -->

  <!-- 
      This template echos out an element but redacts descendant elements
      if they match more specific 'redact' templates.
  -->
  <xsl:template match="*" mode="redact">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="node()" mode="redact"/>
    </xsl:copy>
  </xsl:template>

  <!-- ====================================================================== -->

  <!-- 
      Text nodes of elements that are retained are output unchanged in
      "redact" mode.
  -->
  <xsl:template match="text" mode="redact">
    <xsl:copy-of select="."/>
  </xsl:template>

  <!-- ====================================================================== -->
  
  <!-- This 'redact' template is specialised to processing elements that
  have an 'id' attribute which is a blackboard database user ID. -->
  <xsl:template match="USER" mode="redact">
    <xsl:variable name="id" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@id"/>
    <xsl:choose>
      <xsl:when test="not( @id = $id )">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy-of select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================================== -->

  <!-- This 'redact' template is specialised to processing elements that
  have a COURSEMEMBERSHIPID child element. -->
  <xsl:template match="OUTCOME | GROUPMEMBERSHIP" mode="redact">
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:choose>
      <xsl:when test="count( COURSEMEMBERSHIPID[@value = $memberid] ) = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy-of select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================================== -->

  <!-- This 'redact' template is specialised to processing elements that
  have a USERID child element. -->
  <xsl:template match="GRADE_HISTORY_ENTRY | COURSEMEMBERSHIP" mode="redact">
    <xsl:variable name="id" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@id"/>
    <xsl:choose>
      <xsl:when test="count( USERID[@value = $id] ) = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy-of select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================================== -->

  <!-- This 'redact' template is specialised to processing elements that
  have a USERID child element. -->
  <xsl:template match="DATE_LOGGED" mode="redact">
    <xsl:variable name="epoch" select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DateProcessor.epoch( @value )"/>
    <xsl:copy>
      <xsl:attribute name="bbcarv:epoch"><xsl:value-of select="$epoch"/></xsl:attribute>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*" mode="redact"/>
    </xsl:copy>
  </xsl:template>

  <!-- ====================================================================== -->

  <!-- This 'redact' template is specialised to processing elements that
  have an ATTEMPTID child element that needs to be looked up in the gradebook data. -->
  <xsl:template match="ATTEMPTFILE | ATTEMPTEMBEDDEDFILE" mode="redact">
    <xsl:variable name="attemptid" select="./ATTEMPTID/@value"/>
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="memberattempts" select="count(./ancestor::bbcarv:studentandfiles//OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/../ATTEMPTS/ATTEMPT[@id = $attemptid])"/>
    <xsl:choose>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy-of select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================================== -->
  <xsl:template match="wiki" mode="redact">
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="gid" select="groupID/text()"/>
    <xsl:variable name="gmships" select="./ancestor::bbcarv:studentandfiles/bbcarv:resources/bbcarv:resource/bbcarv:data/GROUPMEMBERSHIPS/GROUPMEMBERSHIP[GROUPID/@value = $gid and COURSEMEMBERSHIPID/@value = $memberid]"/>
    
    <xsl:choose>
      <xsl:when test="count( $gmships ) = 0">
        <xsl:comment>Whole wiki redacted - student not member of group.</xsl:comment>        
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy-of select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================================== -->

  <!-- 
       This 'redact' template is specialised to file elements. This is complicated
       because the method of redaction depends on the type of resource where the
       element might appear.
  -->
  <xsl:template match="bbcarv:file" mode="redact">
    <xsl:variable name="base" select="../@xml:base"/>
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="context" select="./ancestor::bbcarv:resource/@type"/>
    <xsl:variable name="fileid" select="substring-before( @href, '/' )"/>
    <!-- attemptid depends on the type of resource in the imsmanifest -->
    <xsl:variable name="attemptid">
      <xsl:choose>
        <xsl:when test="$context = 'course/x-bb-attemptfiles'">
          <xsl:value-of select="./ancestor::bbcarv:studentandfiles//ATTEMPTFILES/ATTEMPTFILE[@id = $fileid]/ATTEMPTID/@value"/>
        </xsl:when>
        <xsl:when test="$context = 'course/x-bb-attemptembeddedfiles'">
          <xsl:value-of select="substring-before( @href, '/' )"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'xxxxxxxxxxx'"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="memberattempts" select="count(./ancestor::bbcarv:studentandfiles//OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/../ATTEMPTS/ATTEMPT[@id = $attemptid])"/>

    <xsl:choose>
      <xsl:when test="$context = 'resource/x-bb-wiki'">
        <!-- Assumes that whole wikis can be redacted. If wiki is included, include all its files. -->
        <xsl:copy-of select="."/>
      </xsl:when>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <!-- ====================================================================== -->
  
  <xsl:template match="cat:catalogue" mode="redact">
  </xsl:template>
  
  <!-- ====================================================================== -->
  
  <xsl:template match="cat:catalogue" mode="catredact">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="node()" mode="catredact"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- ====================================================================== -->

  <!-- 
       This 'redact' template is specialised to catalogue file elements, i.e.
       files that are in the original zip file. The idea is to redact all but
       files related to the current student: their submissions, their feedback
       and tutor notes.
  -->
  <xsl:template match="cat:file" mode="catredact">
    <xsl:choose>
      <xsl:when test="cat:dir[1]/@name = 'csfiles'">
        <xsl:apply-templates select="." mode="redact-csfiles"/>
      </xsl:when>
      <xsl:otherwise>      
        <xsl:apply-templates select="." mode="redact-notcsfiles"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="cat:file" mode="redact-csfiles">
    <xsl:choose>
      <xsl:when test="cat:dir[2]/@name = 'privatedoc' and cat:dir[3]/@name = 'gradebook' and cat:dir[4]/@name = 'notesAndFeedback'">
        <xsl:apply-templates select="." mode="redact-notesandfeedback"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- <xsl:comment>Redacted catalogue file. Unknown type of csfile</xsl:comment> -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-notesandfeedback">
    <xsl:choose>
      <xsl:when test="cat:dir[5]/@name = 'attempt' or cat:dir[5]/@name = 'attemptNotes'">
        <xsl:apply-templates select="." mode="redact-attempt"/>
      </xsl:when>
      <xsl:when test="cat:dir[5]/@name = 'gradeHistory' or cat:dir[5]/@name = 'gradeHistoryNotes' or cat:dir[5]/@name = 'gradeDetail'  or cat:dir[5]/@name = 'gradeDetailNotes'">
        <xsl:apply-templates select="." mode="redact-outcome"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- <xsl:comment>Redacted catalogue file. Gradebook file of unknown type.</xsl:comment> -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-attempt">
    <xsl:variable name="attemptid" select="cat:dir[6]/@name"/>
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="memberattempts" select="count(./ancestor::bbcarv:studentandfiles//OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/../ATTEMPTS/ATTEMPT[@id = $attemptid])"/>
    <xsl:variable name="dsturi" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@rel-uri"/>
    <xsl:choose>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Redacted catalogue file. Relates to other student's attempt.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <log><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor.processFile( $file-processor-id, @uri, $dsturi )"/></log>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-outcome">
    <xsl:variable name="outcomeid" select="cat:dir[6]/@name"/>
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="memberattempts" select="count(./ancestor::bbcarv:studentandfiles//OUTCOME[@id = $outcomeid]/COURSEMEMBERSHIPID[@value = $memberid])"/>
    <xsl:variable name="dsturi" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@rel-uri"/>
    <xsl:choose>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Redacted catalogue file. Relates to other student's outcome.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <log><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor.processFile( $file-processor-id, @uri, $dsturi )"/></log>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-notcsfiles">
    <xsl:variable name="sub" select="cat:dir[1]/@name"/>
    <xsl:variable name="restype" select="/manifest/resources/resource[ @xml:base = $sub ]/@type"/>
    <xsl:choose>
      <xsl:when test="$restype = 'course/x-bb-attemptfiles'">
        <xsl:apply-templates select="." mode="redact-resattempt"/>
      </xsl:when>
      <xsl:when test="$restype = 'course/x-bb-attemptembeddedfiles'">
        <xsl:apply-templates select="." mode="redact-resattemptembed"/>
      </xsl:when>
      <xsl:when test="$restype = 'resource/x-bb-wiki'">
        <xsl:apply-templates select="." mode="redact-reswiki"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:comment>Redacted catalogue file. Not in csfiles and not in an interesting resource subfolder. <xsl:value-of select="@uri"/></xsl:comment>
      </xsl:otherwise>      
    </xsl:choose>
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-resattempt">
    <xsl:variable name="attemptfileid" select="cat:dir[2]/@name"/>
    <xsl:variable name="attemptid" select="./ancestor::bbcarv:studentandfiles//ATTEMPTFILES/ATTEMPTFILE[@id = $attemptfileid]/ATTEMPTID/@value"/>
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="memberattempts" select="count(./ancestor::bbcarv:studentandfiles//OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/../ATTEMPTS/ATTEMPT[@id = $attemptid])"/>
    <xsl:variable name="dsturi" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@rel-uri"/>
    <xsl:choose>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Redacted catalogue file. Relates to other student's attempt file.</xsl:comment>        
      </xsl:when>
      <xsl:otherwise>
        <log><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor.processFile( $file-processor-id, @uri, $dsturi )"/></log>
      </xsl:otherwise>
    </xsl:choose>       
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-resattemptembed">
    <xsl:variable name="attemptid" select="cat:dir[2]/@name"/>
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="memberattempts" select="count(./ancestor::bbcarv:studentandfiles//OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/../ATTEMPTS/ATTEMPT[@id = $attemptid])"/>
    <xsl:variable name="dsturi" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@rel-uri"/>
    <xsl:choose>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Redacted catalogue file. Relates to other student's attempt embedded file.</xsl:comment>        
      </xsl:when>
      <xsl:otherwise>
        <log><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor.processFile( $file-processor-id, @uri, $dsturi )"/></log>
      </xsl:otherwise>
    </xsl:choose>       
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-reswiki">
    <!--
    <xsl:variable name="memberid" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@memberid"/>
    <xsl:variable name="sub" select="cat:dir[1]/@name"/>
    <xsl:variable name="wiki" select="/manifest/resources/resource[ @xml:base = $sub and @type = 'course/x-bb-wiki' ]"/>
    <xsl:variable name="gid" select="$wiki/groupID/text()"/>
    <xsl:variable name="gmships" select="./ancestor::bbcarv:studentandfiles/bbcarv:resources/bbcarv:resource/bbcarv:data/GROUPMEMBERSHIPS/GROUPMEMBERSHIP[GROUPID/@value = $gid and COURSEMEMBERSHIPID/@value = $memberid]"/>
-->
    <xsl:variable name="dsturi" select="./ancestor::bbcarv:studentandfiles/bbcarv:student/@rel-uri"/>
    <!--
    <xsl:choose>
      <xsl:when test="count( $gmships ) = 0">
        <xsl:comment>Redacted catalogue file. Relates to group student is not in.</xsl:comment>        
      </xsl:when>
      <xsl:otherwise>
        -->
        <log><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor.processFile( $file-processor-id, @uri, $dsturi )"/></log>
        <!--
      </xsl:otherwise>
    </xsl:choose>       
          -->
  </xsl:template>


  <!-- ====================================================================== -->

  <!-- 
    Named template that converts student ID into a path name for
    a student's redacted data file.
  -->
  <xsl:template name="make-student-uri">
    <xsl:param name="sid"/>
    <xsl:value-of select="$output-base-uri" />
    <xsl:text>\</xsl:text>
    <xsl:value-of select="substring($sid,1,2)" />
    <xsl:text>______\</xsl:text>
    <xsl:value-of select="substring($sid,1,4)" />
    <xsl:text>____\</xsl:text>
    <xsl:value-of select="substring($sid,1,6)" />
    <xsl:text>__\</xsl:text>
    <xsl:value-of select="$sid" />
    <xsl:text>\bbarchives\</xsl:text>
    <xsl:value-of select="$unredacted/bbcarv:resources/bbcarv:resource/bbcarv:data/COURSE/COURSEID/@value" />
    <xsl:text>\</xsl:text>
  </xsl:template>
  
  <xsl:template name="make-student-relative-uri">
    <xsl:param name="sid"/>
    <xsl:value-of select="substring($sid,1,2)" />
    <xsl:text>______/</xsl:text>
    <xsl:value-of select="substring($sid,1,4)" />
    <xsl:text>____/</xsl:text>
    <xsl:value-of select="substring($sid,1,6)" />
    <xsl:text>__/</xsl:text>
    <xsl:value-of select="$sid" />
    <xsl:text>/bbarchives/</xsl:text>
    <xsl:value-of select="$unredacted/bbcarv:resources/bbcarv:resource/bbcarv:data/COURSE/COURSEID/@value" />
    <xsl:text>/</xsl:text>
  </xsl:template>
  
</xsl:stylesheet>
