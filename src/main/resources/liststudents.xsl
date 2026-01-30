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
  xmlns:bbcarv="http://www.leedsbeckett.ac.uk/bubblecarver"
  xmlns:cat="http://www.leedsbeckett.ac.uk/bubblecarver/catalogue"
  xmlns:java="http://xml.apache.org/xalan/java"
  extension-element-prefixes="java"
  version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="mapper-id" select="'unknown'"/>
  <xsl:param name="data-lookups-id" select="'unknown'"/>

  <xsl:template match="/">
    <archive-log>
      <xsl:apply-templates select="/manifest/resources/resource[ @type = 'course/x-bb-user' ]" mode="user"/>
      <xsl:apply-templates select="/manifest/resources/resource[ @type = 'membership/x-bb-coursemembership' ]" mode="member"/>
      <xsl:apply-templates select="/manifest/resources/resource[ @type = 'course/x-bb-gradebook' ]" mode="gradebook"/>
      <xsl:apply-templates select="/manifest/resources/resource[ @type = 'course/x-bb-attemptfiles' ]" mode="attemptfiles"/>
      <xsl:apply-templates select="document('catalogue:')/cat:catalogue"/>
    </archive-log>
  </xsl:template>

  <xsl:template match="resource" mode="user">
    <xsl:apply-templates select="document( ./@bb:file )/USERS/USER"/>
  </xsl:template>
      
  <xsl:template match="resource" mode="member">
    <xsl:apply-templates select="document( ./@bb:file )/COURSEMEMBERSHIPS/COURSEMEMBERSHIP"/>
  </xsl:template>

  <xsl:template match="resource" mode="gradebook">
    <xsl:apply-templates select="document( ./@bb:file )/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME"/>
  </xsl:template>

  <xsl:template match="resource" mode="attemptfiles">
    <xsl:apply-templates select="document( ./@bb:file )/ATTEMPTFILES/ATTEMPTFILE"/>
  </xsl:template>

  <xsl:template match="USER">
    <xsl:variable name="ignore" 
       select="java:uk.ac.leedsbeckett.bubblecarver.xsl.ModuleUserInfoMapper.addUser( $mapper-id, @id, STUDENTID/@value )"/>
    <u x="{$ignore}"/>
  </xsl:template>
  
  <xsl:template match="COURSEMEMBERSHIP">
    <xsl:variable name="ignore" 
       select="java:uk.ac.leedsbeckett.bubblecarver.xsl.ModuleUserInfoMapper.addMembership( $mapper-id, USERID/@value, @id )"/>
    <m x="{$ignore}"/>
  </xsl:template>
  
  <xsl:template match="OUTCOME">
    <xsl:variable name="mid" select="COURSEMEMBERSHIPID/@value"/>
    <xsl:for-each select="ATTEMPTS/ATTEMPT">
      <xsl:variable name="aid" select="@id"/>
      <xsl:variable name="ignorea" 
         select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.setParentChild( $data-lookups-id, 'member-attempt', $mid, $aid )"/>
      <a x="{$ignorea}"/>
      <xsl:for-each select="RESULTOBJECTID">
        <xsl:variable name="roid" select="@value"/>
        <xsl:if test="string-length( $roid ) &gt; 0">
          <xsl:variable name="ignore" 
             select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.setParentChild( $data-lookups-id, 'member-resultobj', $mid, $roid )"/>
          <o x="{$ignore}"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>  

  <xsl:template match="ATTEMPTFILE">
    <xsl:variable name="aid" select="ATTEMPTID/@value"/>
    <xsl:variable name="fid" select="@id"/>
    <xsl:variable name="ignore" 
       select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.setParentChild( $data-lookups-id, 'attempt-file', $aid, $fid )"/>
    <af x="{$ignore}"/>    
  </xsl:template>  

<!-- ==================================================================== -->

  <xsl:template match="cat:catalogue">
    <xsl:apply-templates select="cat:file"/>
  </xsl:template>

  <xsl:template match="cat:file">
   <xsl:choose>
      <xsl:when test="cat:dir[1]/@name = 'csfiles'">
        <xsl:apply-templates select="." mode="redact-csfiles"/>
      </xsl:when>
      <xsl:otherwise>
        
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
        <!-- <xsl:apply-templates select="." mode="redact-outcome"/> -->
      </xsl:when>
      <xsl:otherwise>
        <!-- <xsl:comment>Redacted catalogue file. Gradebook file of unknown type.</xsl:comment> -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="cat:file" mode="redact-attempt">
    <xsl:variable name="aid" select="cat:dir[6]/@name"/>
    <xsl:variable name="mid"
      select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.getParentOfChild( $data-lookups-id, 'member-attempt', $aid )"/>
    <xsl:variable name="ignore" 
       select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookupHelper.setParentChild( $data-lookups-id, 'member-uri', $mid, @uri )"/>
    <af x="{$ignore}"/>        
  </xsl:template>

      
</xsl:stylesheet>
