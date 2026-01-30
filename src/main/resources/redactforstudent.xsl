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

  <xsl:param name="data-lookups-id" select="'unknown'"/>
  <xsl:param name="file-processor-id" select="'unknown'"/>
  <xsl:param name="uid" select="'unknown'"/>
  <xsl:param name="sid" select="'unknown'"/>
  <xsl:param name="mid" select="'unknown'"/>

  <!-- 
      The entry point template that starts the processing of the imsmanifest
      input file. It iterates students found in the unredacted data and for
       each one outputs a redacted version of the data with other students'
      data removed.
  -->
  <xsl:template match="/">
    <xsl:text>
</xsl:text>

    <bbcarv:student uid="{$uid}" sid="{$sid}" memberid="{$mid}">
      <xsl:apply-templates select="bbcarv:resources"/>
    </bbcarv:student>

  </xsl:template>
  
  <xsl:template match="bbcarv:resources">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*"/>
    </xsl:copy>        
  </xsl:template>
  
  <xsl:template match="bbcarv:resource">
    <xsl:copy-of select="."/>
  </xsl:template>

    
  <!-- ================================================================== -->
  
  
  <xsl:template match="bbcarv:resource[@type = 'assessment/x-bb-qti-attempt']">
    <xsl:variable name="gradebookoutdef" 
      select="/bbcarv:resources/bbcarv:resource[@type = 'course/x-bb-gradebook']/bbcarv:data/GRADEBOOK/OUTCOMEDEFINITIONS"/>
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <!-- don't bother with the resource data, just want the file content -->
      <xsl:for-each select="bbcarv:file">
        <!-- The file name (href) is resultobjectid value from the grade centre attempt -->
        <xsl:variable name="href" select="@href"/>
        <xsl:variable name="midofro"
          select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-resultobj', $href )"/>
        <xsl:if test="$midofro = $mid">
          <xsl:copy-of select="."/>
        </xsl:if>
      </xsl:for-each>
    </xsl:copy>        
  </xsl:template>
  
  <!-- ================================================================== -->

  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-attemptfiles']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactattemptfiles"/>
    </xsl:copy>    
  </xsl:template>
  
  <xsl:template match="ATTEMPTFILE" mode="redactattemptfiles">
    <xsl:variable name="fid" select="@id"/>
    <xsl:variable name="attemptoffile"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'attempt-file', $fid )"/>
    <xsl:variable name="midofa"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-attempt', $attemptoffile )"/>
    <xsl:if test="$midofa = $mid">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:template> 

  <xsl:template match="bbcarv:file" mode="redactattemptfiles">
    <xsl:variable name="fid" select="substring-before( @href, '/' )"/>
    <xsl:variable name="attemptoffile"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'attempt-file', $fid )"/>
    <xsl:variable name="midofa"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-attempt', $attemptoffile )"/>
    <xsl:if test="$midofa = $mid">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:template> 

  <xsl:template match="*" mode="redactattemptfiles">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactattemptfiles"/>
    </xsl:copy>        
  </xsl:template>
  
  <!-- ================================================================== -->

  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-attemptembeddedfiles']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactattemptEfiles"/>
    </xsl:copy>    
  </xsl:template>
  
  <xsl:template match="ATTEMPTEMBEDDEDFILE" mode="redactattemptEfiles">
    <xsl:variable name="attemptoffile"
      select="ATTEMPTID/@value"/>
    <xsl:variable name="midofa"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-attempt', $attemptoffile )"/>
    <xsl:if test="$midofa = $mid">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:template> 

  <xsl:template match="bbcarv:file" mode="redactattemptEfiles">
    <xsl:variable name="attemptoffile" select="substring-before( @href, '/' )"/>
    <xsl:variable name="midofa"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-attempt', $attemptoffile )"/>
    <xsl:if test="$midofa = $mid">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:template> 

  <xsl:template match="*" mode="redactattemptEfiles">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactattemptEfiles"/>
    </xsl:copy>        
  </xsl:template>
  
  <!-- ================================================================== -->

  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-coursemessage']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactmessages"/>
    </xsl:copy>    
  </xsl:template>
  
  
  <xsl:template match="COURSEMESSAGE" mode="redactmessages">
    <xsl:variable name="parts" select="PARTICIPANTS/PARTICIPANT/USERID[@value = $uid]"/>
    <xsl:if test="count( $parts ) &gt; 0">
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:copy-of select="."/>
        </xsl:for-each>
        <xsl:apply-templates select="*|text()" mode="redactmessages"/>
      </xsl:copy>        
    </xsl:if>
  </xsl:template>
  
  
  <xsl:template match="*" mode="redactmessages">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactmessages"/>
    </xsl:copy>        
  </xsl:template>
  
  <!-- ================================================================== -->

  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-crsrubriceval']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="rubriceval"/>
    </xsl:copy>    
  </xsl:template>
  
  <!-- Might be linked to an attempt directly or might be linked to
       a grade book history entry. -->
  <xsl:template match="RUBRIC_EVALUATION" mode="rubriceval">
    <xsl:variable name="aid" select="ATTEMPT_ID/@value"/>
    <xsl:variable name="logid" select="GRADEBOOK_LOG_ID/@value"/>
    <xsl:choose>
      <xsl:when test="string-length( $aid ) &gt; 0">
        <xsl:variable name="midofa"
          select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-attempt', $aid )"/>
        <xsl:if test="$midofa = $mid">
          <xsl:copy-of select="."/>
        </xsl:if>        
      </xsl:when>
      <xsl:when test="string-length( $logid ) &gt; 0">
        <xsl:comment>Redacted rubric evaluation that is kept because only for a grade book log entry.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:comment>Redacted rubric evaluation that is connected neither to attempt of grade book log entry.</xsl:comment>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="*" mode="rubriceval">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="rubriceval"/>
    </xsl:copy>        
  </xsl:template>
  
      
  <!-- ================================================================== -->
  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-user']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactuser"/>
    </xsl:copy>       
  </xsl:template>
  
  <xsl:template match="USER" mode="redactuser">
    <xsl:variable name="useruid" select="@id"/>
    <xsl:if test="$useruid = $uid">
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:copy-of select="."/>
        </xsl:for-each>
        <xsl:apply-templates select="*|text()" mode="redactuser"/>
      </xsl:copy>
    </xsl:if>   
  </xsl:template>

  <xsl:template match="BIRTH" mode="redactuser">
  </xsl:template>
    
  <xsl:template match="ADDRESS" mode="redactuser">
  </xsl:template>
    
  <xsl:template match="PHONES" mode="redactuser">
  </xsl:template>

  <xsl:template match="WEBPAGE" mode="redactuser">
  </xsl:template>
    
  <xsl:template match="*" mode="redactuser">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactuser"/>
    </xsl:copy>        
  </xsl:template>
  
  
  <xsl:template match="bbcarv:resource[@type = 'membership/x-bb-coursemembership']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <bbcarv:data>
        <COURSEMEMBERSHIPS>
          <xsl:copy-of select="bbcarv:data/COURSEMEMBERSHIPS/COURSEMEMBERSHIP[@id = $mid]"/>
        </COURSEMEMBERSHIPS>
      </bbcarv:data>
    </xsl:copy>        
  </xsl:template>


  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-review-status']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <bbcarv:data>
        <CONTENTSTATUSES>
          <xsl:for-each select="bbcarv:data/CONTENTSTATUSES/CONTENTSTATUS/USERID[ @value = $uid ]/..">
            <xsl:copy-of select="."/>
          </xsl:for-each>
        </CONTENTSTATUSES>
      </bbcarv:data>
    </xsl:copy>        
  </xsl:template>



  <xsl:template match="bbcarv:resource[@type = 'course/x-bb-gradebook']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactgradebook"/>
    </xsl:copy>        
  </xsl:template>

  <xsl:template match="OUTCOME" mode="redactgradebook">
    <xsl:variable name="outcomemid" select="COURSEMEMBERSHIPID/@value"/>
    <xsl:if test="$outcomemid = $mid">
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:copy-of select="."/>
        </xsl:for-each>
        <xsl:apply-templates select="*|text()" mode="redactgradebook"/>
      </xsl:copy>        
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="GRADE_HISTORY_ENTRY" mode="redactgradebook">
    <xsl:variable name="outcomemid" select="USERID/@value"/>
    <xsl:if test="$outcomemid = $uid">
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:copy-of select="."/>
        </xsl:for-each>
        <xsl:apply-templates select="*|text()" mode="redactgradebook"/>
      </xsl:copy>        
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="*" mode="redactgradebook">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()" mode="redactgradebook"/>
    </xsl:copy>        
  </xsl:template>



            
  <xsl:template match="bbcarv:resource[@type = 'resource/x-bb-achievements']">
    <xsl:variable name="resuid" select="bbcarv:data/achievementNotification/userId"/>
    <xsl:if test="$resuid = $uid">
      <xsl:copy-of select="."/>    
    </xsl:if>
  </xsl:template>
  


  <!-- ================================================================== -->  
            
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()"/>
    </xsl:copy>        
  </xsl:template>

  <xsl:template match="text()">
    <xsl:text>
      <xsl:value-of select="."/>
    </xsl:text>
  </xsl:template>
  
  <!-- ================================================================== -->  

  <xsl:template match="cat:catalogue">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="cat:file">
    <xsl:variable name="filemid" 
       select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-uri', @uri )"/>
    <xsl:if test="$mid = $filemid">
      <cat:file uri="{@uri}"/>
      <log><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.FileProcessor.processFile( $file-processor-id, @uri )"/></log>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
