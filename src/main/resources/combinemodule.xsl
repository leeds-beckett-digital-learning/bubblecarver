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

  <!--
      This variable lists the MIME types of all the resources from the
      imsmanifest that we want to put in the student centred archive
      output. They are concatenated and delimited by new lines.
  -->
  <xsl:variable name="types">
    assessment/x-bb-qti-attempt
    assessment/x-bb-qti-test
    course/x-bb-attemptembeddedfiles
    course/x-bb-attemptfiles
    course/x-bb-availability-rule
    course/x-bb-contenthandler
    course/x-bb-courseassessment
    course/x-bb-coursemessage
    course/x-bb-coursemessagefolder
    course/x-bb-coursesetting
    course/x-bb-crsrubricassocation
    course/x-bb-crsrubriceval
    course/x-bb-gradebook
    course/x-bb-group
    course/x-bb-groupattemptfiles
    course/x-bb-groupupload
    course/x-bb-review-status
    course/x-bb-rubrics
    course/x-bb-user
    course/x-bb-usertestoption
    discussionboard/x-bb-usermsgstate
    membership/x-bb-coursemembership
    membership/x-bb-groupmembership
    resource/x-bb-achievements
    resource/x-bb-attendance
    resource/x-bb-blog
    resource/x-bb-document
    resource/x-bb-retention
    resource/x-bb-task
    resource/x-bb-taskprogress
    resource/x-bb-video-integration
    resource/x-bb-wiki
    resource/x-plugin-scormengine
  </xsl:variable>

  <!-- 
      The entry point template that starts the processing of the imsmanifest
      input file. It iterates students found in the unredacted data and for
       each one outputs a redacted version of the data with other students'
      data removed.
  -->
  <xsl:template match="/">
    <xsl:text>
</xsl:text>
    <bbcarv:resources>
      <xsl:for-each select="/manifest/resources/resource[ contains( $types, @type ) ]">
        <xsl:apply-templates select="."/>
      </xsl:for-each>
      <xsl:copy-of select="document( 'catalogue:' )"/>
    </bbcarv:resources>
  </xsl:template>

  <xsl:template match="resource">
    <bbcarv:resource type="{@type}" xml:base="{@xml:base}" identifier="{@identifier}">
      <bbcarv:data>
        <xsl:copy-of select="document( ./@bb:file )"/>
      </bbcarv:data>          
      <xsl:for-each select="file">
        <bbcarv:file>
          <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
        </bbcarv:file>
      </xsl:for-each>
    </bbcarv:resource>
  </xsl:template>

  <xsl:template match="resource[@type  = 'resource/x-bb-document']">
    <xsl:variable name="identifier" select="@identifier"/>
    <xsl:variable name="bbdoc" select="document( ./@bb:file )"/>
    <xsl:variable name="contenthandler" select="$bbdoc/CONTENT/CONTENTHANDLER/@value"/>
    <xsl:if test="$contenthandler = 'resource/x-bb-achievement'">
      <bbcarv:resource type="{@type}" xml:base="{@xml:base}" identifier="{@identifier}">
        <bbcarv:data>
          <xsl:copy-of select="$bbdoc"/>
        </bbcarv:data>          
        <xsl:for-each select="file">
          <bbcarv:file>
            <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
          </bbcarv:file>
        </xsl:for-each>
      </bbcarv:resource>
    </xsl:if>
  </xsl:template>

  <xsl:template match="resource[@type  = 'course/x-bb-availability-rule']">
    <xsl:variable name="ruleidentifier" select="@identifier"/>
    <xsl:variable name="bbrule" select="document( ./@bb:file )"/>
    <xsl:variable name="rulecontentid" select="$bbrule/RULE/CONTENT_ID/@value"/>
    <xsl:variable name="docresource" select="/manifest/resources/resource[@identifier = $rulecontentid]"/>
    <xsl:variable name="bbdoc" select="document( $docresource/@bb:file )"/>
    <xsl:variable name="contenthandler" select="$bbdoc/CONTENT/CONTENTHANDLER/@value"/>
    
    <xsl:if test="$contenthandler = 'resource/x-bb-achievement'">
      <bbcarv:resource type="{@type}" xml:base="{@xml:base}" identifier="{@identifier}">
        <bbcarv:data>
          <xsl:copy-of select="$bbrule"/>
        </bbcarv:data>          
        <xsl:for-each select="file">
          <bbcarv:file>
            <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
          </bbcarv:file>
        </xsl:for-each>
      </bbcarv:resource>
    </xsl:if>
  </xsl:template>

  <!-- No data file in this case. Only resource files. -->
  <xsl:template match="resource[@type  = 'assessment/x-bb-qti-attempt']">
    <bbcarv:resource type="{@type}" xml:base="{@xml:base}" identifier="{@identifier}">
      <xsl:for-each select="file">
        <bbcarv:file>
          <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
          <xsl:variable name="filehref" select="concat( ../@xml:base, '/', @href )"/>
          <bbcarv:data href="{$filehref}">
            <xsl:copy-of select="document( $filehref ) "/>
          </bbcarv:data>
        </bbcarv:file>
      </xsl:for-each>
    </bbcarv:resource>
  </xsl:template>


</xsl:stylesheet>
