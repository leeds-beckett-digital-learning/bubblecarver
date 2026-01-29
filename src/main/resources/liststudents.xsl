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
    <xsl:for-each select="ATTEMPTS/ATTEMPT/RESULTOBJECTID">
      <xsl:variable name="roid" select="@value"/>
      <xsl:if test="string-length( $roid ) &gt; 0">
        <xsl:variable name="ignore" 
           select="java:uk.ac.leedsbeckett.bubblecarver.xsl.DataLookups.addResultObjectId( $data-lookups-id, $mid, $roid )"/>
        <o x="{$ignore}"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>  
  
</xsl:stylesheet>
