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
  extension-element-prefixes="redirect"
  exclude-result-prefixes="bb"
  version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="output-base-uri" select="'unknown path to nowhere'"/>
  
  <xsl:variable name="coursesettingfile">
    <file>
      <xsl:if test="count(/manifest/resources/resource[@type = 'course/x-bb-coursesetting']) = 1">
        <xsl:copy-of select="document( /manifest/resources/resource[@type = 'course/x-bb-coursesetting']/@bb:file )"/>
      </xsl:if>
    </file>
  </xsl:variable>
    
  <xsl:variable name="usersfile">
    <file>
      <xsl:if test="count(/manifest/resources/resource[@type = 'course/x-bb-user']) = 1">
        <xsl:copy-of select="document( /manifest/resources/resource[@type = 'course/x-bb-user']/@bb:file )"/>
      </xsl:if>
    </file>
  </xsl:variable>

  <xsl:variable name="membershipfile">
    <file>
      <xsl:if test="count(/manifest/resources/resource[@type = 'membership/x-bb-coursemembership']) = 1">
        <xsl:copy-of select="document( /manifest/resources/resource[@type = 'membership/x-bb-coursemembership']/@bb:file )"/>
      </xsl:if>
    </file>
  </xsl:variable>

  <xsl:variable name="gradebookfile">
    <file>
      <xsl:if test="count(/manifest/resources/resource[@type = 'course/x-bb-gradebook']) = 1">
        <xsl:copy-of select="document( /manifest/resources/resource[@type = 'course/x-bb-gradebook']/@bb:file )"/>
      </xsl:if>
    </file>
  </xsl:variable>
        
  <xsl:variable name="attemptfile">
    <file>
      <xsl:for-each select="/manifest/resources/resource[@type = 'course/x-bb-attemptfiles']/@bb:file">
        <xsl:copy-of select="document( . )"/>
      </xsl:for-each>
    </file>
  </xsl:variable>
        
  <xsl:template match="/">
    <xsl:text>
</xsl:text>
    <archive-log>
      <xsl:attribute name="archive"><xsl:value-of select="nodeset( $coursesettingfile )/file/COURSE/COURSEID/@value"/></xsl:attribute>
      <xsl:copy-of select="$attemptfile"/>

      <xsl:for-each select="nodeset( $usersfile )/file/USERS/USER[string-length(STUDENTID/@value) = 8]">
        <xsl:variable name="uid" select="@id"/>
        <xsl:variable name="sid" select="STUDENTID/@value"/>
        <xsl:variable name="student-file">
          <xsl:call-template name="make-student-uri">
            <xsl:with-param name="sid" select="$sid"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="memberid" select="nodeset( $membershipfile )/file/COURSEMEMBERSHIPS/COURSEMEMBERSHIP/USERID[@value = $uid]/../@id"/>
        
        <!-- 
              This variable puts the combined data into a nodeset with
              the current student's IDs near the root. This will allow
              any template to find the current student ID based purely
              on the context node. This is done because we need to use
              apply-templates and there is no mechanism to send parameters.
        -->
        <xsl:variable name="unredactedparameterized">
          <studentandfiles>
            <student>
              <xsl:attribute name="id"><xsl:value-of select="$uid"/></xsl:attribute>
              <xsl:attribute name="memberid"><xsl:value-of select="$memberid"/></xsl:attribute>
            </student>
            <files>
              <xsl:copy-of select="$coursesettingfile"/>
              <xsl:copy-of select="$usersfile"/>
              <xsl:copy-of select="$membershipfile"/>
              <xsl:copy-of select="$gradebookfile"/>
              <xsl:copy-of select="$attemptfile"/>
            </files>           
          </studentandfiles>
        </xsl:variable>
        
        <student-log>
          <xsl:attribute name="student-file"><xsl:value-of select="$student-file"/></xsl:attribute>
        </student-log>
        
        <redirect:write file="{$student-file}">
          <xsl:text>
</xsl:text>
          <student sid="{$sid}" uid="{$uid}" memberid="{$memberid}">
            <xsl:apply-templates select="nodeset( $unredactedparameterized )/studentandfiles/files/file" mode="redact"/>
          </student>
        </redirect:write>
        
      </xsl:for-each>
    </archive-log>
  </xsl:template>


  <!-- Echos out an element but redacts it if it relates -->
  <xsl:template match="*" mode="redact">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="*" mode="redact"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="USER" mode="redact">
    <xsl:variable name="id" select="./ancestor::studentandfiles/student/@id"/>
    <xsl:choose>
      <xsl:when test="not( @id = $id )">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Echos out an element but redacts it if it relates -->
  <xsl:template match="OUTCOME" mode="redact">
    <xsl:variable name="memberid" select="./ancestor::studentandfiles/student/@memberid"/>
    <xsl:choose>
      <xsl:when test="count( COURSEMEMBERSHIPID[@value = $memberid] ) = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="GRADE_HISTORY_ENTRY | COURSEMEMBERSHIP" mode="redact">
    <xsl:variable name="id" select="./ancestor::studentandfiles/student/@id"/>
    <xsl:choose>
      <xsl:when test="count( USERID[@value = $id] ) = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="ATTEMPTFILE" mode="redact">
    <xsl:variable name="attemptid" select="./ATTEMPTID/@value"/>
    <xsl:variable name="memberid" select="./ancestor::studentandfiles/student/@memberid"/>
    <xsl:variable name="memberattempts" select="count(./ancestor::studentandfiles//OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/../ATTEMPTS/ATTEMPT[@id = $attemptid])"/>
    <xsl:choose>
      <xsl:when test="$memberattempts = 0">
        <xsl:comment>Element <xsl:value-of select="name()"/> redacted.</xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:copy select="."/>
          </xsl:for-each>
          <xsl:apply-templates select="*" mode="redact"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>



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
    <xsl:text>\</xsl:text>
    <xsl:value-of select="nodeset( $coursesettingfile )/file/COURSE/COURSEID/@value" />
    <xsl:text>.xml</xsl:text>
  </xsl:template>
  
</xsl:stylesheet>
