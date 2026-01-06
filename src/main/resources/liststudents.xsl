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
        
  <xsl:template match="/">
    <xsl:text>
</xsl:text>
    <students>
      <!--
      <debug>
        <xsl:copy-of select="nodeset( $coursesettingfile )"/>
      </debug>
      -->
      <xsl:for-each select="nodeset( $usersfile )/file/USERS/USER[string-length(STUDENTID/@value) = 8]">
        <xsl:variable name="uid" select="@id"/>
        <xsl:variable name="sid" select="STUDENTID/@value"/>
        <xsl:variable name="student-file">
          <xsl:call-template name="make-student-uri">
            <xsl:with-param name="sid" select="$sid"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="memberid" select="nodeset( $membershipfile )/file/COURSEMEMBERSHIPS/COURSEMEMBERSHIP/USERID[@value = $uid]/../@id"/>
        <xsl:variable name="tree">
          <studentandfiles>
            <student>
              <xsl:attribute name="id"><xsl:value-of select="$uid"/></xsl:attribute>
              <xsl:attribute name="sid"><xsl:value-of select="$sid"/></xsl:attribute>
              <xsl:attribute name="memberid"><xsl:value-of select="$memberid"/></xsl:attribute>
              <xsl:attribute name="student-file"><xsl:value-of select="$student-file"/></xsl:attribute>
            </student>
            <files>
              <xsl:copy-of select="$gradebookfile"></xsl:copy-of>
            </files>           
          </studentandfiles>
        </xsl:variable>
        
        <student>
          <xsl:attribute name="id"><xsl:value-of select="$uid"/></xsl:attribute>
          <xsl:attribute name="sid"><xsl:value-of select="$sid"/></xsl:attribute>
          <xsl:attribute name="memberid"><xsl:value-of select="$memberid"/></xsl:attribute>
          <xsl:attribute name="student-file"><xsl:value-of select="$student-file"/></xsl:attribute>
          <!-- <xsl:copy-of select="$tree"></xsl:copy-of> -->
        </student>
        
        <redirect:write file="{$student-file}">
          <xsl:text>
</xsl:text>
          <student sid="{$sid}" uid="{$uid}" memberid="{$memberid}">
            <USERS>
              <USER id="{$uid}">
                <xsl:copy-of select="NAMES"/>
                <xsl:copy-of select="INSTITUTIONEMAIL"/>
              </USER>
            </USERS>
            <xsl:apply-templates select="nodeset( $tree )/studentandfiles/files/file/GRADEBOOK" mode="redact"/>
          </student>
        </redirect:write>
        
      </xsl:for-each>
    </students>
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

  <xsl:template match="GRADE_HISTORY_ENTRY" mode="redact">
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
