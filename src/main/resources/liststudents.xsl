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
        <student>
          <xsl:attribute name="id"><xsl:value-of select="$uid"/></xsl:attribute>
          <xsl:attribute name="sid"><xsl:value-of select="$sid"/></xsl:attribute>
          <xsl:attribute name="memberid"><xsl:value-of select="$memberid"/></xsl:attribute>
          <xsl:attribute name="student-file"><xsl:value-of select="$student-file"/></xsl:attribute>
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
            <GRADEBOOK>
              <xsl:for-each select="nodeset( $gradebookfile )/file/GRADEBOOK/*">
                <xsl:choose>
                  <xsl:when test="name() = 'OUTCOMEDEFINITIONS'">
                    <OUTCOMEDEFINITIONS>
                      <xsl:for-each select="OUTCOMEDEFINITION">
                        <OUTCOMEDEFINITION id="{@id}">
                          <xsl:for-each select="*">
                            <xsl:choose>
                              <xsl:when test="name() = 'OUTCOMES'">
                                <OUTCOMES>
                                  <xsl:for-each select="OUTCOME/COURSEMEMBERSHIPID[@value = $memberid]/..">
                                    <xsl:copy-of select="."/>                          
                                  </xsl:for-each>
                                </OUTCOMES>
                              </xsl:when>
                              <xsl:otherwise>
                                <xsl:copy-of select="."/>
                              </xsl:otherwise>
                            </xsl:choose>
                          </xsl:for-each>
                        </OUTCOMEDEFINITION>
                      </xsl:for-each>
                    </OUTCOMEDEFINITIONS>
                  </xsl:when>
                  <xsl:when test="name() = 'GRADE_HISTORY_ENTRIES'">
                    <GRADE_HISTORY_ENTRIES>
                      <xsl:for-each select="GRADE_HISTORY_ENTRY/USERID[@value = $uid]/..">
                        <xsl:copy-of select="."/>                
                      </xsl:for-each>                      
                    </GRADE_HISTORY_ENTRIES>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:copy-of select="."/>                    
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </GRADEBOOK>
          </student>
        </redirect:write>
        
      </xsl:for-each>
    </students>
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
